package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import net.minidev.json.annotate.JsonIgnore
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import java.time.LocalDate
import java.time.LocalDateTime

interface TransferRequest {
  val reasonCode: String
  val destinationCode: String?
  val logisticsCode: String?
  val plan: PlanRequest?
  val schedule: ScheduleRequest?
  val movement: MovementRequest?

  fun initialStatusCode(): TransferStatus.Code
  fun initialStage(): TransferStage

  @get:JsonIgnore
  val isReadyToSchedule get() = plan != null && destinationCode != null && logisticsCode != null && schedule?.start != null
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
