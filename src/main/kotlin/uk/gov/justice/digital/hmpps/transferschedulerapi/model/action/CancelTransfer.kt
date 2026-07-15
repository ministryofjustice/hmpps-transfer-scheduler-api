package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferCancelled

data object CancelTransfer : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    entity.cancel(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferCancelled(entity.person.identifier, entity.id)
}
