package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.ScheduleCommentsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.CommentsAction

data class ApplyScheduleComments(override val comments: String?) :
  TransferAction,
  CommentsAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    when (val schedule = entity.schedule) {
      is Schedule -> schedule.applyComments(this)
      else -> throw ConflictException("Cannot apply comments without a schedule")
    }
  }

  override fun domainEvent(entity: Transfer) = ScheduleCommentsChanged(entity.person.identifier, entity.id, entity.stage)
}
