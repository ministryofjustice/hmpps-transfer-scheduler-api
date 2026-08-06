package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class IntegrationResponse(val data: Transfer) {

  @Schema(name = "IntegrationTransfer")
  data class Transfer(
    val id: UUID,
    val personIdentifier: String,
    val prisonCode: String,
    val status: CodedDescription,
    val reason: CodedDescription,
    val destinationCode: String?,
    val logistics: CodedDescription?,
    val plan: Plan?,
    val schedule: Schedule?,
    val movement: Movement?,
  )

  @Schema(name = "IntegrationPlan")
  data class Plan(val requestedOn: LocalDate, val priority: CodedDescription, val comments: String?)

  @Schema(name = "IntegrationSchedule")
  data class Schedule(val start: LocalDateTime, val comments: String?)

  @Schema(name = "IntegrationMovement")
  data class Movement(val occurredAt: LocalDateTime, val comments: String?)
}
