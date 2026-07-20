package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferReprioritised
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException

data class ApplyPriority(val priorityCode: String) : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.plan == null) throw ConflictException("Cannot reprioritise without a plan")
    entity.plan?.applyPriority(this, rdProvider)
  }

  override fun domainEvent(entity: Transfer) = TransferReprioritised(entity.person.identifier, entity.id)
}
