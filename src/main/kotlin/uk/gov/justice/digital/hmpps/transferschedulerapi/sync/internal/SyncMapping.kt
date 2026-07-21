package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.StatusValidator.Companion.PRE_SCHEDULED_STATUSES
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.PLANNING
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyTransit
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.CancelTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.CompleteTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ExpireTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.PlanTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ScheduleTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.history.StatusChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncWaitlist
import java.util.UUID

fun Transfer.updateFrom(request: SyncTransfer, personSummary: PersonSummary, rdProvider: RdProvider): Transfer = apply {
  applyLegacyId(legacyId)
  movePerson(personSummary, requireNotNull(request.syncSchedule?.agyLocId ?: request.syncMovement?.fromAgyLocId))
  applyDestination(ApplyDestination(request.destinationCode))
  applyLogistics(ApplyLogistics(request.logisticsCode), rdProvider)
  applyReason(ApplyReason(request.reasonCode), rdProvider)
  if (status.code == SCHEDULED.name && request.plan != null && request.syncSchedule?.isPending == true) {
    with(request.plan) { applyPlan(PlanTransfer(requestedOn, priorityCode, comments), rdProvider) }
  } else {
    withPlan(request.plan, rdProvider)
  }
  if (request.syncSchedule?.isScheduled == true && request.schedule != null && status.code in PRE_SCHEDULED_STATUSES.map { it.name }) {
    with(request.schedule) { applySchedule(ScheduleTransfer(start, comments), rdProvider) }
  } else {
    withSchedule(request.schedule)
  }
  if (movement == null && request.movement != null) {
    with(request.movement) { applyTransit(ApplyTransit(occurredAt, destinationCode, reasonCode, logisticsCode, comments), rdProvider) }
  } else {
    withMovement(request.movement, rdProvider)
  }

  when {
    request.isCancelled -> cancel(CancelTransfer, rdProvider)
    request.isExpired -> expire(ExpireTransfer, rdProvider)
    request.isCompleted || (request.syncSchedule == null && request.syncMovement?.active != true) -> complete(CompleteTransfer, rdProvider)
    request.isReadyToSchedule && status.code == PLANNING.name -> applyStatus(READY_TO_SCHEDULE, rdProvider)
    !request.isReadyToSchedule && status.code == READY_TO_SCHEDULE.name -> applyStatus(PLANNING, rdProvider)
  }
}

fun Movement.syncIdsFromLegacyId(): Pair<Long, Long>? {
  val parts = legacyId?.split("_")
  return if (parts?.size != 2) {
    null
  } else {
    parts[0].toLong() to parts[1].toLong()
  }
}

fun Transfer.toSyncModel(statusChanges: (UUID) -> List<StatusChanged>): SyncTransfer = SyncTransfer(
  id,
  legacyId,
  syncWaitList(statusChanges),
  syncSchedule(),
  syncMovement(),
)

fun Transfer.syncWaitList(
  statusChanges: (UUID) -> List<StatusChanged>,
) = plan?.let {
  val statusChanges = statusChanges(it.id).sortedByDescending { sc -> sc.occurredAt }
  val mostRecent = statusChanges.firstOrNull { sc -> sc.to in setOf(PLANNING, READY_TO_SCHEDULE, SCHEDULED) }
  val approvedBy = if (stage == TransferStage.SCHEDULED) {
    statusChanges.firstOrNull { sc -> sc.to == SCHEDULED }
  } else {
    null
  }
  SyncWaitlist(
    it.requestedOn,
    statusForWaitlist(),
    mostRecent?.occurredAt?.toLocalDate() ?: it.requestedOn,
    it.priority.code,
    status.code == SCHEDULED.name,
    approvedBy?.username,
    if (status.code == TransferStatus.Code.CANCELLED.name) SyncWaitlist.OutcomeReasonCode.ADMI else null,
    it.comments,
  )
}

fun Transfer.syncSchedule() = if (stage == TransferStage.UNSCHEDULED) {
  null
} else {
  SyncSchedule(
    schedule?.start,
    requireNotNull(reason).code,
    statusForSchedule(),
    schedule?.comments,
    null,
    prisonCode,
    destinationCode,
    null,
    logistics?.code,
  )
}

fun Transfer.syncMovement(): SyncMovement? = movement?.let {
  val legacyIdParts = it.syncIdsFromLegacyId()
  SyncMovement(
    legacyIdParts?.first,
    legacyIdParts?.second,
    it.occurredAt,
    it.reason.code,
    it.logistics.code,
    prisonCode,
    it.destinationCode,
    null,
    it.comments,
  )
}

fun Transfer.statusForWaitlist(): String = when (TransferStatus.Code.valueOf(status.code)) {
  TransferStatus.Code.CANCELLED -> SyncWaitlist.CANCELLED
  SCHEDULED -> SyncWaitlist.CONFIRMED
  else -> SyncWaitlist.PENDING
}

fun Transfer.statusForSchedule(): String = when (TransferStatus.Code.valueOf(status.code)) {
  TransferStatus.Code.COMPLETED -> SyncSchedule.COMPLETED
  TransferStatus.Code.CANCELLED -> SyncSchedule.CANCELLED
  TransferStatus.Code.EXPIRED -> SyncSchedule.EXPIRED
  SCHEDULED -> SyncSchedule.SCHEDULED
  else -> SyncSchedule.PENDING
}
