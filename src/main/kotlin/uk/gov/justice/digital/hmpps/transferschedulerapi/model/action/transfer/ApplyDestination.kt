package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.PreScheduleStatusAction

data class ApplyDestination(val destinationCode: String?) :
  TransferAction,
  PreScheduleStatusAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    entity.applyDestination(this)
    updatePreScheduleStatus(entity, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferRelocated(entity.person.identifier, entity.id, entity.stage)
}
