package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PlanRequestedOnChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import java.time.LocalDate

data class ApplyRequestedOn(val requestedOn: LocalDate) : TransferAction {
  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    when (val plan = entity.plan) {
      is Plan -> plan.applyRequestedOn(this)
      else -> throw ConflictException("Cannot apply requested on without a plan")
    }
  }

  override fun domainEvent(entity: Transfer) = PlanRequestedOnChanged(entity.person.identifier, entity.id, entity.stage)
}
