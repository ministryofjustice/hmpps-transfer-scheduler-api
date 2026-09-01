package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.COMPLETED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.IN_TRANSIT
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.PLANNING
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
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

val PRE_SCHEDULED_STATUSES: Set<TransferStatus.Code> = setOf(PLANNING, READY_TO_SCHEDULE)

fun Transfer.updateFrom(request: SyncTransfer, personSummary: PersonSummary, rdProvider: RdProvider): Transfer = apply {
  applyLegacyId(legacyId)
  movePerson(personSummary)
  if (movement == null) {
    movePrison(request.syncSchedule.agyLocId)
  }
  applyDestination(ApplyDestination(request.destinationCode))
  applyLogistics(ApplyLogistics(request.logisticsCode), rdProvider)
  applyReason(ApplyReason(request.reasonCode), rdProvider)
  if (status.code == SCHEDULED.name && request.plan != null && request.syncSchedule.isPending) {
    with(request.plan) { applyPlan(PlanTransfer(requestedOn, priorityCode, comments), rdProvider) }
  } else {
    withPlan(request.plan, rdProvider)
  }
  if (request.syncSchedule.isScheduled && request.schedule != null && status.code in PRE_SCHEDULED_STATUSES.map { it.name }) {
    with(request.schedule) { applySchedule(ScheduleTransfer(start, comments), rdProvider) }
  } else {
    withSchedule(request.schedule)
  }

  when {
    request.isCancelled -> cancel(CancelTransfer, rdProvider)
    request.isExpired -> expire(ExpireTransfer, rdProvider)
    request.isReadyToSchedule && status.code == PLANNING.name -> applyStatus(READY_TO_SCHEDULE, rdProvider)
    !request.isReadyToSchedule && status.code == READY_TO_SCHEDULE.name -> applyStatus(PLANNING, rdProvider)
  }
}

fun Transfer.toSyncModel(
  statusChanges: (UUID) -> List<StatusChanged>,
  legacyDataProvider: (UUID) -> LegacyData?,
): SyncTransfer = SyncTransfer(
  id,
  legacyId,
  syncWaitList(statusChanges, legacyDataProvider),
  syncSchedule(),
)

fun Transfer.syncWaitList(
  statusChanges: (UUID) -> List<StatusChanged>,
  legacyDataProvider: (UUID) -> LegacyData?,
) = plan?.let {
  val statusChanges = statusChanges(it.id).sortedByDescending { sc -> sc.occurredAt }
  val mostRecent = statusChanges.firstOrNull { sc -> sc.to in setOf(PLANNING, READY_TO_SCHEDULE, SCHEDULED) }
  val approvedBy = if (stage == TransferStage.SCHEDULED) {
    statusChanges.firstOrNull { sc -> sc.to == SCHEDULED }
  } else {
    null
  }
  val legacyData = legacyDataProvider(it.id)
  SyncWaitlist(
    it.requestedOn,
    statusForWaitlist(),
    mostRecent?.occurredAt?.toLocalDate() ?: legacyData?.waitList?.statusDate ?: it.requestedOn,
    it.priority.code,
    status.code in setOf(SCHEDULED.name, IN_TRANSIT.name, COMPLETED.name) || legacyData?.waitList?.approved == true,
    approvedBy?.username ?: legacyData?.waitList?.approvedUsername,
    if (status.code == TransferStatus.Code.CANCELLED.name) {
      legacyData?.waitList?.outcomeReasonCodeAsEnum() ?: SyncWaitlist.OutcomeReasonCode.ADMI
    } else {
      null
    },
    it.comments,
  )
}

fun Transfer.syncSchedule() = SyncSchedule(
  schedule?.start,
  reason.code,
  statusForSchedule(),
  schedule?.comments,
  null,
  prisonCode,
  destinationCode,
  null,
  logistics?.code,
)

fun Transfer.statusForWaitlist(): String = when (TransferStatus.Code.valueOf(status.code)) {
  TransferStatus.Code.CANCELLED -> SyncWaitlist.CANCELLED
  SCHEDULED -> SyncWaitlist.CONFIRMED
  else -> SyncWaitlist.PENDING
}

fun Transfer.statusForSchedule(): String = when (TransferStatus.Code.valueOf(status.code)) {
  COMPLETED -> SyncSchedule.COMPLETED
  TransferStatus.Code.CANCELLED -> SyncSchedule.CANCELLED
  TransferStatus.Code.EXPIRED -> SyncSchedule.EXPIRED
  SCHEDULED -> SyncSchedule.SCHEDULED
  else -> SyncSchedule.PENDING
}

fun Transfer.addMovement(request: SyncMovement, rdProvider: RdProvider): Movement {
  if (movement != null) throw ConflictException("Movement already exists")
  if (request.active == true) {
    applyTransit(ApplyTransit(request), rdProvider)
  } else {
    withMovement(request, rdProvider).complete(CompleteTransfer, rdProvider)
  }
  return requireNotNull(movement)
}

fun Movement.syncIdsFromLegacyId(): Pair<Long, Int>? {
  val parts = legacyId?.split("_")
  return if (parts?.size != 2) {
    null
  } else {
    parts[0].toLong() to parts[1].toInt()
  }
}

fun Movement.syncMovement(): SyncMovement {
  val legacyIdParts = syncIdsFromLegacyId()
  return SyncMovement(
    id,
    transfer.takeIf { it.stage == TransferStage.SCHEDULED }?.id,
    legacyIdParts?.first,
    legacyIdParts?.second,
    occurredAt,
    reason.code,
    logistics.code,
    transfer.prisonCode,
    destinationCode,
    transfer.status.code == IN_TRANSIT.name,
    comments,
  )
}

fun Movement.updateFrom(request: SyncMovement, transfer: Transfer, rdProvider: RdProvider) = apply {
  applyTransfer(transfer)
  match(request, rdProvider)
  if (request.active != true && transfer.status.code != COMPLETED.name) {
    transfer.complete(CompleteTransfer, rdProvider)
  }
}
