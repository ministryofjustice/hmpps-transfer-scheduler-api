package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyTransit
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.CancelTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.CompleteTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ExpireTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.PlanTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ScheduleTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncWaitlist
import java.time.LocalDate

fun Transfer.updateFrom(request: SyncTransfer, personSummary: PersonSummary, rdProvider: RdProvider): Transfer = apply {
  movePerson(personSummary, requireNotNull(request.syncSchedule?.agyLocId ?: request.syncMovement?.fromAgyLocId))
  applyDestination(ApplyDestination(request.destinationCode))
  applyLogistics(ApplyLogistics(request.logisticsCode), rdProvider)
  applyReason(ApplyReason(request.reasonCode), rdProvider)
  if (plan == null && request.plan != null) {
    with(request.plan) { applyPlan(PlanTransfer(requestedOn, priorityCode, comments), rdProvider) }
  } else {
    withPlan(request.plan, rdProvider)
  }
  if (schedule == null && request.schedule != null) {
    with(request.schedule) { applySchedule(ScheduleTransfer(start, comments), rdProvider) }
  } else {
    withSchedule(request.schedule)
  }
  if (movement == null && request.movement != null) {
    with(request.movement) { applyTransit(ApplyTransit(occurredAt, comments), rdProvider) }
  } else {
    withMovement(request.movement)
  }
  when {
    request.isCompleted -> complete(CompleteTransfer, rdProvider)
    request.isCancelled -> cancel(CancelTransfer, rdProvider)
    request.isExpired -> expire(ExpireTransfer, rdProvider)
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

fun Transfer.toSyncModel(): SyncTransfer = SyncTransfer(
  id,
  legacyId,
  syncWaitList(),
  syncSchedule(),
  syncMovement(),
)

// TODO: status date and approved staff id need information from audit columns
fun Transfer.syncWaitList() = plan?.let {
  SyncWaitlist(
    it.requestedOn,
    statusForWaitlist(),
    LocalDate.now(),
    it.priority.code,
    status.code == TransferStatus.Code.SCHEDULED.name,
    null,
    null,
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

fun Transfer.syncMovement(): SyncMovement? = movement?.let {
  val legacyIdParts = it.syncIdsFromLegacyId()
  SyncMovement(
    legacyIdParts?.first,
    legacyIdParts?.second,
    it.occurredAt,
    reason.code,
    requireNotNull(logistics?.code),
    prisonCode,
    requireNotNull(destinationCode),
    null,
    it.comments,
  )
}

fun Transfer.statusForWaitlist(): String = when (TransferStatus.Code.valueOf(status.code)) {
  TransferStatus.Code.CANCELLED -> SyncWaitlist.CANCELLED
  TransferStatus.Code.SCHEDULED -> SyncWaitlist.CONFIRMED
  else -> SyncWaitlist.PENDING
}

fun Transfer.statusForSchedule(): String = when (TransferStatus.Code.valueOf(status.code)) {
  TransferStatus.Code.COMPLETED -> SyncSchedule.COMPLETED
  TransferStatus.Code.CANCELLED -> SyncSchedule.CANCELLED
  TransferStatus.Code.EXPIRED -> SyncSchedule.EXPIRED
  TransferStatus.Code.SCHEDULED -> SyncSchedule.SCHEDULED
  else -> SyncSchedule.PENDING
}
