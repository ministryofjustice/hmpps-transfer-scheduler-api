package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest.Companion.SCHEDULE_START
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest.Companion.STATUS

data class PersonTransferSearchRequest(
  override val statusCodes: Set<TransferStatus.Code> = emptySet(),
  override val reasonCodes: Set<String> = emptySet(),
  override val page: Int = 1,
  override val size: Int = 50,
  override val sort: String = SCHEDULE_START,
) : TransferSearchRequest {
  override fun validSortFields(): Set<String> = setOf(SCHEDULE_START, STATUS)
}
