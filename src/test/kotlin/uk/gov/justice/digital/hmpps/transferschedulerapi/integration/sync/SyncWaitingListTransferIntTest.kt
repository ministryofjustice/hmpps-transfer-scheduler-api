package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferPlanned
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferPlanned.Companion.invoke
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransferResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncUser
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst

class SyncWaitingListTransferIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYNC, personIdentifier())
      .bodyValue(syncTransferRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    sendTransfer(
      personIdentifier(),
      syncTransfer(waitlist = syncWaitList(), schedule = syncSchedule()),
      syncUser(),
      Roles.TRANSFER_SCHEDULER_UI,
    )
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if schedule and movement are missing`() {
    sendTransfer(personIdentifier(), syncTransfer(schedule = null, movement = null), syncUser())
      .errorResponse(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `200 - can create a new waiting list transfer`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))

    val request = syncTransfer(waitlist = syncWaitList(), schedule = syncSchedule(start = null, eventStatus = SyncSchedule.PENDING))
    val user = syncUser()
    val res = sendTransfer(prisoner.prisonerNumber, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get().copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(saved, setOf(TransferPlanned(prisoner.prisonerNumber, saved.id, DataSource.NOMIS).publication(saved.id)))
  }

  private fun sendTransfer(
    personIdentifier: String,
    request: SyncTransfer,
    syncUser: SyncUser,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC, personIdentifier)
    .bodyValue(syncTransferRequest(request, syncUser))
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC = "/sync/transfers/{personIdentifier}"
  }
}
