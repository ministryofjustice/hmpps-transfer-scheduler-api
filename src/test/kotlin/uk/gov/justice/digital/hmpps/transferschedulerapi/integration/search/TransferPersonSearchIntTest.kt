package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.PersonTransferSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst

class TransferPersonSearchIntTest(
  @Autowired tro: TransferOperations,
  @Autowired pso: PersonSummaryOperations,
) : IntegrationTestBase(),
  TransferOperations by tro,
  PersonSummaryOperations by pso {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(URL_TO_TEST, prisonCode())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    searchTransfers(prisonCode(), searchRequest(), "ROLE_ANY__OTHER").expectStatus().isForbidden
  }

  @Test
  fun `can filter transfers by reason code`() {
    val personIdentifier = personIdentifier()
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val reasonCode = TransferReasonCode.randomCode()
    val anotherCode = generateSequence { TransferReasonCode.randomCode() }.first { it != reasonCode }

    val toFind =
      givenTransfer(
        transfer(
          personIdentifier = personIdentifier,
          prisonCode = prison.code,
          destinationCode = destination.code,
          reasonCode = reasonCode,
        ),
      )
    givenTransfer(
      transfer(
        personIdentifier = personIdentifier,
        prisonCode = prison.code,
        destinationCode = destination.code,
        reasonCode = anotherCode,
      ),
    )

    val res = searchTransfers(
      personIdentifier,
      searchRequest(reasons = setOf(reasonCode)),
    ).successResponse<TransferSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) { this verifyAgainst toFind }
  }

  @Test
  fun `can filter transfers by status`() {
    val personIdentifier = personIdentifier()
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))

    val inTransit = givenTransfer(
      transfer(
        personIdentifier = personIdentifier,
        prisonCode = prison.code,
        destinationCode = destination.code,
        movement = movement(),
        statusCode = TransferStatus.Code.IN_TRANSIT,
      ),
    )
    assertThat(inTransit.status.code).isEqualTo(TransferStatus.Code.IN_TRANSIT.name)
    val scheduled = givenTransfer(
      transfer(
        personIdentifier = personIdentifier,
        prisonCode = prison.code,
        destinationCode = destination.code,
      ),
    )
    assertThat(scheduled.status.code).isEqualTo(TransferStatus.Code.SCHEDULED.name)

    val res1 = searchTransfers(personIdentifier, searchRequest(statuses = setOf(TransferStatus.Code.IN_TRANSIT)))
      .successResponse<TransferSearchResponse>()

    assertThat(res1.content).hasSize(1)
    assertThat(res1.metadata.totalElements).isEqualTo(1)
    with(res1.content.single()) {
      this verifyAgainst inTransit
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }

    val res2 = searchTransfers(personIdentifier, searchRequest(statuses = setOf(TransferStatus.Code.SCHEDULED)))
      .successResponse<TransferSearchResponse>()

    assertThat(res2.content).hasSize(1)
    assertThat(res2.metadata.totalElements).isEqualTo(1)
    with(res2.content.single()) {
      this verifyAgainst scheduled
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }
  }

  private fun searchRequest(
    statuses: Set<TransferStatus.Code> = setOf(),
    reasons: Set<String> = emptySet(),
    page: Int = 1,
    size: Int = 10,
    sort: String = "start,asc",
  ): PersonTransferSearchRequest = PersonTransferSearchRequest(statuses, reasons, page, size, sort)

  private fun searchTransfers(
    personIdentifier: String,
    request: PersonTransferSearchRequest,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .post()
    .uri(URL_TO_TEST, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/search/people/{personIdentifier}/transfers"
  }
}
