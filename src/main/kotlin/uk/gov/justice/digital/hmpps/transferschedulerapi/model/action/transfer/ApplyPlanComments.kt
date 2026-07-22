package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PlanCommentsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.CommentsAction

data class ApplyPlanComments(override val comments: String?) :
  TransferAction,
  CommentsAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    when (val plan = entity.plan) {
      is Plan -> plan.applyComments(this)
      else -> throw ConflictException("Cannot apply comments without a plan")
    }
  }

  override fun domainEvent(entity: Transfer) = PlanCommentsChanged(entity.person.identifier, entity.id)
}
