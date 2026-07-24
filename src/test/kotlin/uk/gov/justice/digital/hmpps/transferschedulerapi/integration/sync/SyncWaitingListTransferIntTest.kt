package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferCancelled
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferLogisticsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferPlanned
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecategorised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferReprioritised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferPriorityCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransferResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncUser
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncWaitlist
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.syncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.syncWaitList
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDateTime

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
  fun `200 - can create a new waiting list transfer`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))

    val request =
      syncTransfer(waitlist = syncWaitList(), schedule = syncSchedule(eventStatus = SyncSchedule.PENDING))
    val user = syncUser()
    val res = sendTransfer(prisoner.prisonerNumber, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Plan::class.simpleName!!,
        Schedule::class.simpleName!!,
      ),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(TransferPlanned(prisoner.prisonerNumber, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can relocate a planned transfer`() {
    val transfer = givenTransfer(transfer(destinationCode = null, statusCode = TransferStatus.Code.PLANNING))
    val newDestination = prisonCode()

    val request =
      transfer.toTestSyncModel().copy(syncSchedule = transfer.syncSchedule().copy(toAgyLocId = newDestination))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(TransferRelocated(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can remove destination from a planned transfer`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE, schedule = null))

    val request =
      transfer.toTestSyncModel().copy(syncSchedule = transfer.syncSchedule().copy(toAgyLocId = null))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.PLANNING.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(TransferRelocated(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can recategorise a planned transfer`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE))
    val newReason = generateSequence { TransferReasonCode.randomCode() }.first { it != transfer.reason?.code }

    val request =
      transfer.toTestSyncModel().copy(syncSchedule = transfer.syncSchedule().copy(eventSubType = newReason))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(TransferRecategorised(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can change logistics for a planned transfer`() {
    val transfer =
      givenTransfer(transfer(logisticsCode = null, statusCode = TransferStatus.Code.PLANNING))
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.PLANNING.name)
    val newLogistics = TransferLogisticsCode.randomCode()

    val request =
      transfer.toTestSyncModel().copy(syncSchedule = transfer.syncSchedule().copy(escortCode = newLogistics))
    assertThat(request.isReadyToSchedule).isTrue
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(TransferLogisticsChanged(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can remove logistics for a planned transfer`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE))

    val request = transfer.toTestSyncModel().copy(syncSchedule = transfer.syncSchedule().copy(escortCode = null))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.PLANNING.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(TransferLogisticsChanged(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can reprioritise a planned transfer`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE))
    val newPriority =
      generateSequence { TransferPriorityCode.randomCode() }.first { it != transfer.plan?.priority?.code }

    val request = transfer.toTestSyncModel().copy(syncWaitlist = transfer.syncWaitList { emptyList() }!!.copy(transferPriority = newPriority))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved.plan!!,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved.plan!!,
      setOf(TransferReprioritised(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can schedule a planned transfer`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE, schedule = null))
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)

    val request = transfer.toTestSyncModel().copy(
      syncSchedule = transfer.syncSchedule()
        .copy(start = LocalDateTime.now().plusDays(5), eventStatus = SyncSchedule.SCHEDULED),
    )
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.SCHEDULED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.SCHEDULED)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(TransferScheduled(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can cancel a planned transfer`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE, schedule = null))
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)

    val request = transfer.toTestSyncModel().copy(
      syncSchedule = transfer.syncSchedule()
        .copy(start = LocalDateTime.now().plusDays(5), eventStatus = SyncSchedule.CANCELLED),
      syncWaitlist = transfer.syncWaitList { _ -> emptyList() }!!
        .copy(outcomeReasonCode = SyncWaitlist.OutcomeReasonCode.OIC, waitListStatus = SyncWaitlist.CANCELLED),
    )
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.CANCELLED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    saved verifyAgainst request

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!),
      SchedulerContext.get()
        .copy(
          username = user.username,
          caseloadId = user.activeCaseloadId,
          source = DataSource.NOMIS,
          reason = "OIC - Offence In Custody",
        ),
    )

    verifyEventPublications(
      saved,
      setOf(TransferCancelled(transfer.person.identifier, saved.id, saved.stage, DataSource.NOMIS).publication(saved.id)),
    )
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
