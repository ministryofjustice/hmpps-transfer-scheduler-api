package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRescheduled
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ApplyScheduleStart(
  val start: LocalDateTime,
) : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    val previouslyScheduled = entity.schedule != null
    if (previouslyScheduled) {
      entity.schedule?.reschedule(this)
    } else {
      entity.withSchedule(ScheduleTransfer(start, null))
      if (entity.status.code == TransferStatus.Code.PLANNING.name && entity.isReadyToSchedule()) {
        entity.applyStatus(TransferStatus.Code.READY_TO_SCHEDULE, rdProvider)
      }
    }
  }

  override fun domainEvent(entity: Transfer) = TransferRescheduled(entity.person.identifier, entity.id, entity.stage)

  infix fun changes(schedule: Schedule?): Boolean = (schedule?.start?.truncatedTo(ChronoUnit.SECONDS) != start.truncatedTo(ChronoUnit.SECONDS))
}
