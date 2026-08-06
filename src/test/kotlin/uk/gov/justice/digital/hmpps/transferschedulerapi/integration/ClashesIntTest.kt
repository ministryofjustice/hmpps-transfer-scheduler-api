package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.Clash
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashPersonIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashRange
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.RetrieveClashes
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SequencedSet

class ClashesIntTest(
  @Autowired private val trOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by trOps {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .post()
      .uri(CLASHES_URL)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getClashes(clashRequest(), Roles.allExcept(Roles.SCHEDULE_CLASHES_RO).toList()).expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("badRequests")
  fun `400 bad request if request does not include at least one person identifier and one clash range`(request: ClashRequest) {
    getClashes(request).expectStatus().isBadRequest
  }

  @ParameterizedTest
  @MethodSource("clashes")
  fun `200 ok - can detect clashes`(range: ClashRange) {
    val transfer = givenTransfer(transfer(schedule = schedule(start = CLASH_DATE.atTime(10, 0))))

    val res = getClashes(
      clashRequest(
        personIdentifiers = linkedSetOf(clashIdentifier(transfer.person.identifier)),
        ranges = linkedSetOf(range),
      ),
    ).successResponse<ClashResponse>()

    assertThat(res.origin).isEqualTo(RetrieveClashes.ORIGIN)
    val personClash = res.data.single()
    assertThat(personClash.personIdentifier.value).isEqualTo(transfer.person.identifier)
    val clash = personClash.clashes.single()
    assertThat(clash.start).isEqualTo(transfer.schedule!!.start)
    assertThat(clash.end).isEqualTo(transfer.schedule!!.start.withHour(23).withMinute(59))
    assertThat(clash.description).isEqualTo(Clash.Description(transfer.reason.description, "Transfer"))
    assertThat(clash.location.description).isEqualTo(RetrieveClashes.OUTSIDE.description)
    assertThat(clash.additionalInformation).isEqualTo(Clash.AdditionalInformation(transfer.prisonCode))
  }

  @ParameterizedTest
  @MethodSource("noClashes")
  fun `200 ok - returns empty when no clashes`(range: ClashRange) {
    val transfer = givenTransfer(transfer(schedule = schedule(start = CLASH_DATE.atTime(10, 0))))

    val res = getClashes(
      clashRequest(
        personIdentifiers = linkedSetOf(clashIdentifier(transfer.person.identifier)),
        ranges = linkedSetOf(range),
      ),
    ).successResponse<ClashResponse>()

    assertThat(res.origin).isEqualTo(RetrieveClashes.ORIGIN)
    assertThat(res.data).isEmpty()
  }

  private fun getClashes(
    request: ClashRequest,
    roles: List<String> = listOf(Roles.SCHEDULE_CLASHES_RO),
  ) = webTestClient
    .post()
    .uri(CLASHES_URL)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = roles))
    .exchange()

  companion object {
    const val CLASHES_URL = "/search/people/clashes"
    val CLASH_DATE: LocalDate = LocalDate.now().plusDays(7)

    private fun clashIdentifier(personIdentifier: String = personIdentifier()) = ClashPersonIdentifier(ClashPersonIdentifier.Type.PRISON_NUMBER, personIdentifier)

    private fun clashRange(
      start: LocalDateTime = CLASH_DATE.atTime(10, 0),
      end: LocalDateTime = CLASH_DATE.atTime(16, 0),
    ) = ClashRange(start, end)

    private fun clashRequest(
      personIdentifiers: SequencedSet<ClashPersonIdentifier> = linkedSetOf(clashIdentifier()),
      ranges: SequencedSet<ClashRange> = linkedSetOf(clashRange()),
    ) = ClashRequest(personIdentifiers, ranges)

    @JvmStatic
    fun clashes() = listOf(
      clashRange(start = CLASH_DATE.atTime(7, 0), end = CLASH_DATE.atTime(12, 0)),
      clashRange(start = CLASH_DATE.atTime(7, 0), end = CLASH_DATE.atTime(19, 0)),
      clashRange(start = CLASH_DATE.atTime(10, 0), end = CLASH_DATE.atTime(16, 0)),
      clashRange(start = CLASH_DATE.atTime(16, 0), end = CLASH_DATE.atTime(22, 0)),
    )

    @JvmStatic
    fun noClashes() = listOf(
      with(CLASH_DATE.plusDays(1)) {
        clashRange(this.atTime(8, 0), end = this.atTime(17, 0))
      },
      with(CLASH_DATE.minusDays(1)) {
        clashRange(this.atTime(8, 0), end = this.atTime(17, 0))
      },
    )

    @JvmStatic
    fun badRequests() = listOf(
      clashRequest(personIdentifiers = linkedSetOf(), ranges = linkedSetOf()),
      clashRequest(personIdentifiers = linkedSetOf(clashIdentifier()), ranges = linkedSetOf()),
      clashRequest(personIdentifiers = linkedSetOf(), ranges = linkedSetOf(clashRange())),
    )
  }
}
