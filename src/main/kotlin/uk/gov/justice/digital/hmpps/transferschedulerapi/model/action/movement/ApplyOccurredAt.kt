package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementOccurredAtChanged
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ApplyOccurredAt(val occurredAt: LocalDateTime) : MovementAction {
  override fun applyTo(entity: Movement, rdProvider: RdProvider) {
    entity.applyOccurredAt(this)
  }

  override fun domainEvent(entity: Movement) = TransferMovementOccurredAtChanged(entity.transfer.person.identifier, entity.transfer.id, entity.id)

  infix fun changes(movement: Movement): Boolean = movement.occurredAt.truncatedTo(ChronoUnit.SECONDS) != occurredAt.truncatedTo(ChronoUnit.SECONDS)
}
