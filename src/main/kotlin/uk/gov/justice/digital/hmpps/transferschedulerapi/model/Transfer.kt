package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import uk.gov.justice.digital.hmpps.transferschedulerapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Transfer(
  val id: UUID,
  val person: Person,
  val prison: Prison,
  val status: CodedDescription,
  val reason: CodedDescription,
  val destination: Prison?,
  val logistics: CodedDescription?,
  val plan: Plan?,
  val schedule: Schedule?,
  val movement: Movement?,
  val comments: String?,
)

data class Person(val identifier: String)

data class Plan(val requestedOn: LocalDate, val priority: CodedDescription, val comments: String?)

data class Schedule(val start: LocalDateTime, val comments: String?)

data class Movement(val occurredAt: LocalDateTime, val comments: String?)
