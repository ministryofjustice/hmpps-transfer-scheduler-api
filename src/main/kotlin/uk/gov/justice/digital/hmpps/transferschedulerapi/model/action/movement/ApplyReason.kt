package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRecategorised

data class ApplyReason(val reasonCode: String) : MovementAction {
  override fun applyTo(entity: Movement, rdProvider: RdProvider) {
    entity.applyReason(this, rdProvider)
  }

  override fun domainEvent(entity: Movement) = TransferMovementRecategorised(entity.transfer.person.identifier, entity.transfer.id, entity.id)
}
