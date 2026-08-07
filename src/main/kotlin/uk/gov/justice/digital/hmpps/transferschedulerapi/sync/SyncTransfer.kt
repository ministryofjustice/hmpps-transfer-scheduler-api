package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.LegacyData
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SyncTransfer(
  val dpsId: UUID?,
  val eventId: Long?,
  @JsonProperty("waitlist")
  val syncWaitlist: SyncWaitlist?,
  @JsonProperty("schedule")
  val syncSchedule: SyncSchedule,
) : NumericLegacyIdRequest,
  TransferRequest {
  @JsonIgnore
  override val legacyId: Long? = eventId

  @JsonIgnore
  override val reasonCode: String = syncSchedule.eventSubType

  @JsonIgnore
  override val destinationCode: String? = syncSchedule.toAgyLocId

  @JsonIgnore
  override val logisticsCode: String? = syncSchedule.escortCode

  @JsonIgnore
  override val plan: PlanRequest? = syncWaitlist?.let {
    object : PlanRequest {
      override val requestedOn: LocalDate = it.requestDate
      override val priorityCode: String = it.transferPriority
      override val comments: String? = it.commentText1
    }
  }

  @JsonIgnore
  override val schedule: ScheduleRequest? = syncSchedule.start?.let {
    object : ScheduleRequest {
      override val start: LocalDateTime = it
      override val comments: String? = syncSchedule.commentText
    }
  }

  @JsonIgnore
  val isCancelled: Boolean = syncSchedule.isCancelled

  @JsonIgnore
  val isExpired: Boolean = syncSchedule.isExpired

  override fun initialStatusCode(): TransferStatus.Code = when {
    syncSchedule.isCancelled -> TransferStatus.Code.CANCELLED
    syncSchedule.isExpired -> TransferStatus.Code.EXPIRED
    syncSchedule.isCompleted -> TransferStatus.Code.COMPLETED
    syncSchedule.isScheduled -> TransferStatus.Code.SCHEDULED
    isReadyToSchedule -> TransferStatus.Code.READY_TO_SCHEDULE
    else -> TransferStatus.Code.PLANNING
  }

  override fun initialStage(): TransferStage = syncSchedule
    .takeIf { it.isScheduled || it.isExpired || (syncWaitlist?.isCancelled != true && syncSchedule.isCancelled) || it.isCompleted }
    ?.let { TransferStage.SCHEDULED } ?: TransferStage.PLANNING

  fun legacyData(): LegacyData? = if (syncWaitlist?.legacyData() == null && syncSchedule.legacyData() == null) {
    null
  } else {
    LegacyData(syncWaitlist?.legacyData(), syncSchedule.legacyData())
  }
}

data class SyncWaitlist(
  val requestDate: LocalDate,
  val waitListStatus: String,
  val statusDate: LocalDate,
  val transferPriority: String,
  val approved: Boolean,
  val approvedUsername: String?,
  val outcomeReasonCode: OutcomeReasonCode?,
  val commentText1: String?,
) {
  @JsonIgnore
  val isCancelled = waitListStatus == CANCELLED

  @JsonIgnore
  val cancellationReason = outcomeReasonCode?.takeIf { isCancelled }?.let {
    "${it.name} - ${it.description}"
  }

  enum class OutcomeReasonCode(val description: String) {
    OIC("Offence In Custody"),
    ADMI("Administrative"),
    TRANS("Insufficient Transport"),
  }

  fun legacyData() = LegacyData.WaitList(statusDate, approved, approvedUsername, cancellationReason)

  companion object {
    const val CANCELLED = "CAN"
    const val CONFIRMED = "CON"
    const val PENDING = "PEN"
  }
}

data class SyncSchedule(
  val start: LocalDateTime?,
  val eventSubType: String,
  val eventStatus: String,
  val commentText: String?,
  val hiddenCommentText: String?,
  val agyLocId: String,
  val toAgyLocId: String?,
  val outcomeReasonCode: String?,
  val escortCode: String?,
) {
  @JsonIgnore
  val isPending = eventSubType == PENDING

  @JsonIgnore
  val isCancelled = eventStatus == CANCELLED

  @JsonIgnore
  val isExpired = eventStatus == EXPIRED

  @JsonIgnore
  val isScheduled = eventStatus == SCHEDULED

  @JsonIgnore
  val isCompleted = eventStatus == COMPLETED

  fun legacyData() = if (outcomeReasonCode == null && hiddenCommentText == null) {
    null
  } else {
    LegacyData.Schedule(hiddenCommentText, outcomeReasonCode)
  }

  companion object {
    const val CANCELLED = "CANC"
    const val COMPLETED = "COMP"
    const val EXPIRED = "EXP"
    const val SCHEDULED = "SCH"
    const val PENDING = "PEN"
  }
}

data class SyncMovement(
  val dpsId: UUID?,
  val dpsTransferId: UUID?,
  val offenderBookId: Long?,
  val movementSeq: Int?,
  override val occurredAt: LocalDateTime,
  val movementReasonCode: String,
  val escortCode: String,
  val fromAgyLocId: String,
  val toAgyLocId: String,
  val active: Boolean?,
  @JsonProperty("commentText")
  override val comments: String?,
) : StringLegacyIdRequest,
  MovementRequest {

  @JsonIgnore
  override val reasonCode: String = movementReasonCode

  @JsonIgnore
  override val destinationCode: String = toAgyLocId

  @JsonIgnore
  override val logisticsCode: String = escortCode

  @JsonIgnore
  override val legacyId: String? =
    if (offenderBookId == null || movementSeq == null) null else "${offenderBookId}_$movementSeq"
}
