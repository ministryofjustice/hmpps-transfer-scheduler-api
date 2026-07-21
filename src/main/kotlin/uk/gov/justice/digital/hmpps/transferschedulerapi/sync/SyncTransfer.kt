package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ValidSyncTransfer
data class SyncTransfer(
  val dpsId: UUID?,
  val eventId: Long?,
  @JsonProperty("waitlist")
  val syncWaitlist: SyncWaitlist?,
  @JsonProperty("schedule")
  val syncSchedule: SyncSchedule?,
  @JsonProperty("movement")
  val syncMovement: SyncMovement?,
) : NumericLegacyIdRequest,
  TransferRequest {
  @get:JsonIgnore
  override val legacyId: Long? = eventId

  @get:JsonIgnore
  override val reasonCode: String = syncSchedule?.eventSubType ?: syncMovement?.movementReasonCode ?: TRANSFER_REASON

  @get:JsonIgnore
  override val destinationCode: String? = syncSchedule?.toAgyLocId ?: syncMovement?.toAgyLocId

  @get:JsonIgnore
  override val logisticsCode: String? = syncSchedule?.escortCode ?: syncMovement?.escortCode

  @get:JsonIgnore
  override val plan: PlanRequest? = syncWaitlist?.let {
    object : PlanRequest {
      override val requestedOn: LocalDate = it.requestDate
      override val priorityCode: String = it.transferPriority
      override val comments: String? = it.commentText1
    }
  }

  @get:JsonIgnore
  override val schedule: ScheduleRequest? = syncSchedule?.start?.let {
    object : ScheduleRequest {
      override val start: LocalDateTime = it
      override val comments: String? = syncSchedule.commentText
    }
  }

  @get:JsonIgnore
  override val movement: MovementRequest? = syncMovement

  @get:JsonIgnore
  val isCancelled: Boolean = syncSchedule?.isCancelled ?: syncWaitlist?.isCancelled ?: false

  @get:JsonIgnore
  val isExpired: Boolean = syncSchedule?.isExpired ?: false

  @get:JsonIgnore
  val isCompleted: Boolean = syncSchedule?.isCompleted ?: false

  override fun initialStatusCode(): TransferStatus.Code = when {
    syncSchedule?.isCancelled == true -> TransferStatus.Code.CANCELLED
    syncSchedule?.isExpired == true -> TransferStatus.Code.EXPIRED
    syncMovement?.active == true -> TransferStatus.Code.IN_TRANSIT
    syncSchedule?.isCompleted == true || syncMovement?.active == false -> TransferStatus.Code.COMPLETED
    syncSchedule?.isScheduled == true -> TransferStatus.Code.SCHEDULED
    isReadyToSchedule -> TransferStatus.Code.READY_TO_SCHEDULE
    else -> TransferStatus.Code.PLANNING
  }

  override fun initialStage(): TransferStage = syncSchedule?.takeIf { it.isScheduled || it.isExpired || it.isCancelled || it.isCompleted }
    ?.let { TransferStage.SCHEDULED } ?: syncMovement?.let { TransferStage.UNSCHEDULED } ?: TransferStage.PLANNING

  companion object {
    private const val TRANSFER_REASON = "TRN"
  }
}

data class SyncWaitlist(
  val requestDate: LocalDate,
  val waitListStatus: String,
  val statusDate: LocalDate,
  val transferPriority: String,
  val approved: Boolean,
  val approvedUsername: String?,
  val outcomeReasonCode: String?,
  val commentText1: String?,
) {
  @JsonIgnore
  val isCancelled = waitListStatus == CANCELLED

  companion object {
    const val CANCELLED = "CAN"
    const val CONFIRMED = "CON"
    const val PENDING = "PEN"

    const val CANCELLED_OUTCOME = "ADMI"
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

  companion object {
    const val CANCELLED = "CANC"
    const val COMPLETED = "COMP"
    const val EXPIRED = "EXP"
    const val SCHEDULED = "SCH"
    const val PENDING = "PEN"
  }
}

data class SyncMovement(
  val offenderBookId: Long?,
  val movementSeq: Long?,
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
  override val legacyId: String? =
    if (offenderBookId == null || movementSeq == null) null else "${offenderBookId}_$movementSeq"
}
