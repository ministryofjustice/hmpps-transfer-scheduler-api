package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.PLANNING
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.valueOf
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException

class StatusValidator(val transfer: Transfer) {
  infix fun valid(status: TransferStatus): TransferStatus {
    if (transfer.status.code == status.code) return transfer.status
    return transfer moveTo status
  }

  private infix fun Transfer.moveTo(newStatus: TransferStatus): TransferStatus {
    val next = valueOf(newStatus.code)
    PREDICATES[next]?.also {
      if (!it(this)) {
        throw ConflictException("Cannot move to $next")
      }
    }
    return newStatus
  }

  companion object {
    val PRE_SCHEDULED_STATUSES: Set<TransferStatus.Code> = setOf(PLANNING, READY_TO_SCHEDULE)

    private val PREDICATES: Map<TransferStatus.Code, (Transfer) -> Boolean> = mapOf(
      READY_TO_SCHEDULE to { tr -> tr.isReadyToSchedule() },
      SCHEDULED to { with(it) { logistics != null && destinationCode != null && schedule != null } },
    )
  }
}
