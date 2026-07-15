package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.PLANNING
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferCancelled
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException

data object CancelTransfer : TransferAction {
  private val VALID_STATUSES = setOf(SCHEDULED.name, READY_TO_SCHEDULE.name, PLANNING.name, CANCELLED.name)

  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.status.code !in VALID_STATUSES) {
      throw ConflictException("Cannot cancel from ${entity.status.code}")
    }
    entity.cancel(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferCancelled(entity.person.identifier, entity.id)
}
