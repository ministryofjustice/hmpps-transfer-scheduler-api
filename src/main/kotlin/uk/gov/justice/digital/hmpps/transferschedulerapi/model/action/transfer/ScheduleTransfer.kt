package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ScheduleTransfer(
  override val start: LocalDateTime,
  override val comments: String?,
) : TransferAction,
  ScheduleRequest {
  companion object {
    private val VALID_STATUSES = setOf(READY_TO_SCHEDULE.name, SCHEDULED.name)
  }

  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.status.code !in VALID_STATUSES) {
      throw ConflictException("Cannot move to scheduled from ${entity.status.code}")
    }
    entity.applySchedule(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferScheduled(entity.person.identifier, entity.id, entity.stage)

  infix fun changes(schedule: Schedule?): Boolean = (schedule?.start?.truncatedTo(ChronoUnit.SECONDS) != start.truncatedTo(ChronoUnit.SECONDS)) || (schedule.comments != comments)
}
