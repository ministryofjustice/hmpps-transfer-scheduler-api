package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.IN_TRANSIT
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferInTransit
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import java.time.LocalDateTime

data class ApplyTransit(
  override val occurredAt: LocalDateTime,
  override val comments: String?,
) : TransferAction,
  MovementRequest {
  companion object {
    private val VALID_STATUSES = setOf(IN_TRANSIT.name, SCHEDULED.name)
  }

  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.status.code !in VALID_STATUSES) {
      throw ConflictException("Cannot move to in transit from ${entity.status.code}")
    }
    entity.applyTransit(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferInTransit(entity.person.identifier, entity.id)

  infix fun changes(movement: Movement?): Boolean = (movement?.occurredAt != occurredAt) || (movement.comments != comments)
}
