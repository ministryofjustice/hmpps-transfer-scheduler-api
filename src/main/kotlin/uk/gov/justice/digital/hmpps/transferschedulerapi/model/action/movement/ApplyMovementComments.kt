package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementCommentsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.CommentsAction

data class ApplyMovementComments(override val comments: String?) :
  MovementAction,
  CommentsAction {
  override fun applyTo(entity: Movement, rdProvider: RdProvider) {
    entity.applyComments(this)
  }

  override fun domainEvent(entity: Movement) = TransferMovementCommentsChanged(entity.transfer.person.identifier, entity.transfer.id, entity.id)
}
