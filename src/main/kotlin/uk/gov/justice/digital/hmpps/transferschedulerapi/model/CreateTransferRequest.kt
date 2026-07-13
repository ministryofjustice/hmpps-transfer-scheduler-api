package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import java.time.LocalDate
import java.time.LocalDateTime

@ValidTransferRequest
data class CreateTransferRequest(
  override val reasonCode: String,
  override val destinationCode: String?,
  override val logisticsCode: String?,
  override val plan: CreatePlanRequest?,
  override val schedule: CreateScheduleRequest?,
) : TransferRequest {
  override fun initialStatusCode(): TransferStatus.Code = when {
    plan == null -> TransferStatus.Code.SCHEDULED
    destinationCode != null && logisticsCode != null -> TransferStatus.Code.READY_TO_SCHEDULE
    else -> TransferStatus.Code.PLANNING
  }
}

data class CreatePlanRequest(
  override val requestedOn: LocalDate,
  override val priorityCode: String,
  override val comments: String?,
) : PlanRequest

data class CreateScheduleRequest(override val start: LocalDateTime, override val comments: String?) : ScheduleRequest
