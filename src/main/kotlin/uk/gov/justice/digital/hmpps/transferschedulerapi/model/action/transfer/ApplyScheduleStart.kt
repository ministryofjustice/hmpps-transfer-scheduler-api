package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRescheduled
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ApplyScheduleStart(
  val start: LocalDateTime,
) : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    val previouslyScheduled = entity.schedule != null
    if (previouslyScheduled) {
      entity.schedule?.reschedule(this)
      if (entity.status.code == TransferStatus.Code.EXPIRED.name && start.isAfter(LocalDateTime.now())) {
        val plan = with(entity) {
          PlanTransfer(
            plan?.requestedOn ?: LocalDate.now(),
            plan?.priority?.code ?: TransferPriority.Code.LOW.value,
            plan?.comments,
          )
        }
        entity.applyPlan(plan, rdProvider)
      } else if (entity.status.code == TransferStatus.Code.SCHEDULED.name && !start.isAfter(LocalDateTime.now())) {
        entity.applyStatus(TransferStatus.Code.EXPIRED, rdProvider)
      }
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
