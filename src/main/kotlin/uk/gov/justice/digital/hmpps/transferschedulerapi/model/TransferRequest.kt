package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import java.time.LocalDate
import java.time.LocalDateTime

interface TransferRequest {
  val reasonCode: String
  val destinationCode: String?
  val logisticsCode: String?
  val plan: CreatePlanRequest?
  val schedule: CreateScheduleRequest?

  fun initialStatusCode(): TransferStatus.Code
  fun initialStage(): TransferStage
}

interface PlanRequest {
  val requestedOn: LocalDate
  val priorityCode: String
  val comments: String?
}

interface ScheduleRequest {
  val start: LocalDateTime
  val comments: String?
}

interface MovementRequest {
  val occurredAt: LocalDateTime
  val comments: String?
}
