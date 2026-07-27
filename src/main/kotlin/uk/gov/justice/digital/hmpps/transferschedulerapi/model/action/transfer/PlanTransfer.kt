package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovedToPlanning
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import java.time.LocalDate

data class PlanTransfer(
  override val requestedOn: LocalDate,
  override val priorityCode: String,
  override val comments: String?,
) : TransferAction,
  PlanRequest {
  companion object {
    private val VALID_STATUSES = setOf(SCHEDULED.name, READY_TO_SCHEDULE.name)
  }

  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.status.code !in VALID_STATUSES) {
      throw ConflictException("Cannot move to planning from ${entity.status.code}")
    }
    entity.applyPlan(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferMovedToPlanning(entity.person.identifier, entity.id, entity.stage)

  infix fun changes(plan: Plan?): Boolean = (plan?.requestedOn != requestedOn) || (plan.priority.code != priorityCode) || (plan.comments != comments)
}
