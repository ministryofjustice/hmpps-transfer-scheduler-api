package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferLogisticsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.PreScheduleStatusAction

data class ApplyLogistics(val logisticsCode: String?) :
  TransferAction,
  PreScheduleStatusAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    entity.applyLogistics(this, rdProvider)
    updatePreScheduleStatus(entity, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferLogisticsChanged(entity.person.identifier, entity.id)
}
