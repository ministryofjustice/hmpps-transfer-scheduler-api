package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferPrisonSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TransferPrisonSearchIntTest(
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
  fun `400 bad request if date range more than 31 days`() {
    val res = searchTransfers(prisonCode(), searchRequest(start = LocalDate.now(), end = LocalDate.now().plusDays(32)))
      .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Invalid date range")
  }

  @Test
  fun `can find transfers by date`() {
    val prison = prisonRegister.givenPrison()
    val start = LocalDate.now().plusDays(2)
    val end = start.plusDays(3)

    val transfers = (0..5).map {
      val startDateTime = LocalDateTime.of(start.minusDays(1).plusDays(it.toLong()), LocalTime.of(10, 0))
      givenTransfer(
        transfer(
          prisonCode = prison.code,
          schedule = if (it == 4) null else schedule(startDateTime),
          reasonCode = TransferReasonCode.randomCode(),
        ),
      )
    }

    val res = searchTransfers(prison.code, searchRequest(start = start, end = end))
      .successResponse<TransferSearchResponse>()
    assertThat(res.content).hasSize(4)
    assertThat(res.metadata.totalElements).isEqualTo(4)
    assertThat(res.content.map { it.id }).containsExactlyElementsOf(transfers.subList(1, 5).map { it.id })
  }

  @Test
  fun `can filter transfers by prison code`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))

    val toFind = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code))
    givenTransfer(transfer())

    val res = searchTransfers(prison.code, searchRequest()).successResponse<TransferSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) {
      this verifyAgainst toFind
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }
  }

  @Test
  fun `can filter transfers by destination code`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))

    val toFind = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code))
    givenTransfer(transfer(prisonCode = prison.code))

    val res = searchTransfers(prison.code, searchRequest(destinations = setOf(destination.code)))
      .successResponse<TransferSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) {
      this verifyAgainst toFind
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }
  }

  @Test
  fun `can filter transfers by person identifier`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))

    val toFind = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code))
    givenTransfer(transfer(prisonCode = prison.code))

    val res = searchTransfers(prison.code, searchRequest(query = toFind.person.identifier))
      .successResponse<TransferSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) {
      this verifyAgainst toFind
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }
  }

  @Test
  fun `can filter transfers by person name`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))

    val toFind = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code))
    givenTransfer(transfer(prisonCode = prison.code))

    toFind.person.nameFormats().forEach {
      val res = searchTransfers(prison.code, searchRequest(query = it))
        .successResponse<TransferSearchResponse>()

      assertThat(res.content).hasSize(1)
      assertThat(res.metadata.totalElements).isEqualTo(1)
      with(res.content.single()) {
        this verifyAgainst toFind
        assertThat(this.prison.code).isEqualTo(prison.code)
        assertThat(this.destination?.code).isEqualTo(destination.code)
      }
    }
  }

  @Test
  fun `person not resident is not found`() {
    val prison = prisonRegister.givenPrison()
    val anotherPrisonCode = prisonCode()

    val fPerson = givenPersonSummary(personSummary(prisonCode = prison.code))
    val nfPerson = givenPersonSummary(personSummary(prisonCode = anotherPrisonCode))

    val fApp = givenTransfer(
      transfer(
        prisonCode = prison.code,
        personIdentifier = fPerson.identifier,
      ),
    )
    givenTransfer(
      transfer(
        prisonCode = prison.code,
        personIdentifier = nfPerson.identifier,
      ),
    )

    val res = searchTransfers(prison.code, searchRequest()).successResponse<TransferSearchResponse>()
    assertThat(res.content.size).isEqualTo(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) {
      assertThat(person.identifier).isEqualTo(fPerson.identifier)
      assertThat(id).isEqualTo(fApp.id)
    }
  }

  @Test
  fun `can filter transfers by reason code`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val reasonCode = TransferReasonCode.randomCode()
    val anotherCode = generateSequence { TransferReasonCode.randomCode() }.first { it != reasonCode }

    val toFind =
      givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, reasonCode = reasonCode))
    givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, reasonCode = anotherCode))

    val res = searchTransfers(prison.code, searchRequest(reasons = setOf(reasonCode)))
      .successResponse<TransferSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) {
      this verifyAgainst toFind
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }
  }

  @Test
  fun `can filter transfers by logistics code`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val logisticsCode = TransferLogisticsCode.randomCode()
    val anotherCode = generateSequence { TransferLogisticsCode.randomCode() }.first { it != logisticsCode }

    val toFind = givenTransfer(
      transfer(
        prisonCode = prison.code,
        destinationCode = destination.code,
        logisticsCode = logisticsCode,
      ),
    )
    givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, logisticsCode = anotherCode))

    val res = searchTransfers(prison.code, searchRequest(logistics = setOf(logisticsCode)))
      .successResponse<TransferSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    with(res.content.single()) {
      this verifyAgainst toFind
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }
  }

  @Test
  fun `can filter transfers by stage`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))

    val planning = givenTransfer(
      transfer(
        prisonCode = prison.code,
        destinationCode = destination.code,
        statusCode = TransferStatus.Code.READY_TO_SCHEDULE,
      ),
    )
    assertThat(planning.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    val scheduled = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code))
    assertThat(scheduled.status.code).isEqualTo(TransferStatus.Code.SCHEDULED.name)

    val res1 = searchTransfers(prison.code, searchRequest(stage = TransferStage.PLANNING))
      .successResponse<TransferSearchResponse>()

    assertThat(res1.content).hasSize(1)
    assertThat(res1.metadata.totalElements).isEqualTo(1)
    with(res1.content.single()) {
      this verifyAgainst planning
      assertThat(this.prison.code).isEqualTo(prison.code)
      assertThat(this.destination?.code).isEqualTo(destination.code)
    }

    val res2 = searchTransfers(prison.code, searchRequest(stage = TransferStage.SCHEDULED))
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
    start: LocalDate = LocalDate.now(),
    end: LocalDate = start.plusDays(30),
    query: String? = null,
    statuses: Set<TransferStatus.Code> = setOf(
      TransferStatus.Code.SCHEDULED,
      TransferStatus.Code.IN_TRANSIT,
      TransferStatus.Code.COMPLETED,
    ),
    reasons: Set<String> = emptySet(),
    destinations: Set<String> = emptySet(),
    logistics: Set<String> = emptySet(),
    priority: TransferPriority.Code? = null,
    stage: TransferStage? = null,
    page: Int = 1,
    size: Int = 10,
    sort: String = "start,asc",
  ) = TransferPrisonSearchRequest(
    start,
    end,
    query,
    statuses,
    reasons,
    destinations,
    logistics,
    priority,
    stage,
    page,
    size,
    sort,
  )

  private fun searchTransfers(
    prisonCode: String,
    request: TransferPrisonSearchRequest,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .post()
    .uri(URL_TO_TEST, prisonCode)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val URL_TO_TEST = "/search/prisons/{prisonCode}/transfers"
  }
}
