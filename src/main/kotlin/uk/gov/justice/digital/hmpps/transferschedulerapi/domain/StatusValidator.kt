package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.PLANNING
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.valueOf

class StatusValidator(val transfer: Transfer) {
  infix fun valid(status: TransferStatus): TransferStatus {
    if (transfer.status.code == status.code) return transfer.status
    return transfer.status moveTo status
  }

  private infix fun TransferStatus.moveTo(status: TransferStatus): TransferStatus {
    val prev = valueOf(code)
    val next = valueOf(status.code)
    // TODO: validate status moves
    return status
  }

  companion object {
    val PRE_SCHEDULED_STATUSES: Set<TransferStatus.Code> = setOf(PLANNING, READY_TO_SCHEDULE)
  }
}
