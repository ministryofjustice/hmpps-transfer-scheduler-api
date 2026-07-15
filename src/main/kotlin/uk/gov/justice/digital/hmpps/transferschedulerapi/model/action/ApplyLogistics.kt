package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferLogisticsChanged

data class ApplyLogistics(val logisticsCode: String?) : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    entity.applyLogistics(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferLogisticsChanged(entity.person.identifier, entity.id)
}
