package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import java.time.LocalDate
import java.time.LocalDateTime

@ValidTransferRequest
data class CreateTransferRequest(
  override val reasonCode: String,
  override val destinationCode: String?,
  override val logisticsCode: String?,
  override val plan: CreatePlanRequest?,
  override val schedule: CreateScheduleRequest?,
  override val comments: String?,
) : TransferRequest

data class CreatePlanRequest(
  override val requestedOn: LocalDate,
  override val priorityCode: String,
  override val comments: String?,
) : PlanRequest

data class CreateScheduleRequest(override val start: LocalDateTime, override val comments: String?) : ScheduleRequest
