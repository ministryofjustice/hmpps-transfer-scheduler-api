package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.COMPLETED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.IN_TRANSIT
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException

data object MakeUnscheduled : TransferAction {
  private val VALID_STATUSES = setOf(IN_TRANSIT.name, COMPLETED.name)

  override fun applyTo(entity: Transfer, rdProvider: RdProvider) {
    if (entity.status.code !in VALID_STATUSES) {
      throw ConflictException("Cannot make unscheduled from ${entity.status.code}")
    }
    entity.makeUnscheduled(this, rdProvider)
  }
}
