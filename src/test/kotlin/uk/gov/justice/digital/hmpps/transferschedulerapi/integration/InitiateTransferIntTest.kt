package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferPlanned
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferPriorityCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.CreatePlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.CreateScheduleRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.CreateTransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDate
import java.time.LocalDateTime

class InitiateTransferIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(INITIATE_TRANSFER_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    initiateTransfer(personIdentifier(), role = "ROLE_ANY__OTHER").expectStatus().isForbidden
  }

  @Test
  fun `400 - bad request if neither plan nor schedule provided`() {
    val res = initiateTransfer(personIdentifier(), transferRequest(plan = null, schedule = null))
      .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Either a plan or schedule must be specified.")
  }

  @Test
  fun `400 - bad request if destination is not a valid prison`() {
    val person = prisonerSearch.givenPrisoner(prisoner(prisonCode()))
    prisonRegister.givenPrison(prison(code = person.lastPrisonId!!))
    val res = initiateTransfer(person.prisonerNumber, transferRequest(destinationCode = "UNK"))
      .errorResponse(HttpStatus.BAD_REQUEST)

    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Invalid request")
    assertThat(res.developerMessage).startsWith("IllegalArgumentException:")
  }

  @Test
  fun `201 created - transfer is initiated with plan and schedule information`() {
    val prison = prison()
    val destination = prison()
    val person = prisonerSearch.givenPrisoner(prisoner(prison.code))
    prisonRegister.givenPrisons(setOf(prison, destination))

    val username = username()
    val request = transferRequest(destinationCode = destination.code)
    val res = initiateTransfer(person.prisonerNumber, request, username, prison.code)
      .successResponse<Transfer>(HttpStatus.CREATED)

    val saved = requireNotNull(findTransfer(res.id))
    saved verifyAgainst request
    res verifyAgainst saved

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Plan::class.simpleName!!, Schedule::class.simpleName!!),
      SchedulerContext.get().copy(username = username, caseloadId = prison.code),
    )

    verifyEventPublications(saved, setOf(TransferScheduled(person.prisonerNumber, saved.id).publication(saved.id)))
  }

  @Test
  fun `201 created - transfer is initiated with plan`() {
    val prison = prison()
    val destination = prison()
    val person = prisonerSearch.givenPrisoner(prisoner(prison.code))
    prisonRegister.givenPrisons(setOf(prison, destination))

    val username = username()
    val request = transferRequest(destinationCode = destination.code, schedule = null)
    val res = initiateTransfer(person.prisonerNumber, request, username, prison.code)
      .successResponse<Transfer>(HttpStatus.CREATED)

    val saved = requireNotNull(findTransfer(res.id))
    saved verifyAgainst request
    res verifyAgainst saved

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get().copy(username = username, caseloadId = prison.code),
    )

    verifyEventPublications(saved, setOf(TransferPlanned(person.prisonerNumber, saved.id).publication(saved.id)))
  }

  @Test
  fun `201 created - transfer is initiated with schedule but no plan`() {
    val prison = prison()
    val destination = prison()
    val person = prisonerSearch.givenPrisoner(prisoner(prison.code))
    prisonRegister.givenPrisons(setOf(prison, destination))

    val username = username()
    val request = transferRequest(destinationCode = destination.code, plan = null)
    val res = initiateTransfer(person.prisonerNumber, request, username, prison.code)
      .successResponse<Transfer>(HttpStatus.CREATED)

    val saved = requireNotNull(findTransfer(res.id))
    saved verifyAgainst request
    res verifyAgainst saved

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!),
      SchedulerContext.get().copy(username = username, caseloadId = prison.code),
    )

    verifyEventPublications(saved, setOf(TransferScheduled(person.prisonerNumber, saved.id).publication(saved.id)))
  }

  private fun planRequest(
    requestedOn: LocalDate = LocalDate.now(),
    priorityCode: String = TransferPriorityCode.randomCode(),
    comments: String? = word(30),
  ) = CreatePlanRequest(requestedOn, priorityCode, comments)

  private fun scheduleRequest(
    start: LocalDateTime = LocalDate.now().plusDays(7).atTime(10, 0),
    comments: String? = word(20),
  ) = CreateScheduleRequest(start, comments)

  private fun transferRequest(
    reasonCode: String = TransferReasonCode.randomCode(),
    destinationCode: String? = prisonCode(),
    logisticsCode: String? = TransferLogisticsCode.randomCode(),
    plan: CreatePlanRequest? = planRequest(),
    schedule: CreateScheduleRequest? = scheduleRequest(),
    comments: String? = word(50),
  ) = CreateTransferRequest(reasonCode, destinationCode, logisticsCode, plan, schedule, comments)

  private fun initiateTransfer(
    personIdentifier: String,
    request: CreateTransferRequest = transferRequest(),
    username: String = DEFAULT_USERNAME,
    caseloadId: String? = null,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .post()
    .uri(INITIATE_TRANSFER_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .headers { hc -> caseloadId?.also { hc.put(CaseloadIdHeader.NAME, listOf(it)) } }
    .exchange()

  companion object {
    const val INITIATE_TRANSFER_URL = "/transfers/{personIdentifier}"
  }
}
