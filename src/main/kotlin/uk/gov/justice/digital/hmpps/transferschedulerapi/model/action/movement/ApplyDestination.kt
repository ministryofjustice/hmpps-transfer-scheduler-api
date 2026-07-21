package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.PreScheduleStatusAction

data class ApplyDestination(val destinationCode: String) :
  MovementAction,
  PreScheduleStatusAction {
  override fun applyTo(entity: Movement, rdProvider: RdProvider) {
    entity.applyDestination(this)
  }

  override fun domainEvent(entity: Movement) = TransferMovementRelocated(entity.transfer.person.identifier, entity.id)
}
