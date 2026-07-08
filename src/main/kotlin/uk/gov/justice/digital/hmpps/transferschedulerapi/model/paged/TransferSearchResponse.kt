package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer

data class TransferSearchResponse(
  override val content: List<Transfer>,
  override val metadata: PageMetadata,
) : PagedResponse<Transfer>
