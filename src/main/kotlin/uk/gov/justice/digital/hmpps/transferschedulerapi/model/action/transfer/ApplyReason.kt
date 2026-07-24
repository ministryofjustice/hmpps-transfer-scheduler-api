package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecategorised

data class ApplyReason(val reasonCode: String) : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    entity.applyReason(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferRecategorised(entity.person.identifier, entity.id, entity.stage)
}
