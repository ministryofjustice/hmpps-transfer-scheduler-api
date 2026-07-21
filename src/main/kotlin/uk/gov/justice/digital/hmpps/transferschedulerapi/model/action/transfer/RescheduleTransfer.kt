package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRescheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class RescheduleTransfer(
  val start: LocalDateTime,
) : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.schedule != null) {
      throw ConflictException("Cannot reschedule before scheduling")
    }
    entity.schedule?.reschedule(this)
  }

  override fun domainEvent(entity: Transfer) = TransferRescheduled(entity.person.identifier, entity.id)

  infix fun changes(schedule: Schedule?): Boolean = (schedule?.start?.truncatedTo(ChronoUnit.SECONDS) != start.truncatedTo(ChronoUnit.SECONDS))
}
