package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferExpired
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException

data object ExpireTransfer : TransferAction {
  private val VALID_STATUSES = setOf(SCHEDULED.name, EXPIRED.name)

  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.status.code !in VALID_STATUSES) {
      throw ConflictException("Cannot cancel from ${entity.status.code}")
    }
    entity.expire(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferExpired(entity.person.identifier, entity.id)
}
