package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementLogisticsChanged

data class ApplyLogistics(val logisticsCode: String) : MovementAction {
  override fun applyTo(entity: Movement, rdProvider: RdProvider) {
    entity.applyLogistics(this, rdProvider)
  }

  override fun domainEvent(entity: Movement) = TransferMovementLogisticsChanged(entity.transfer.person.identifier, entity.transfer.id, entity.id)
}
