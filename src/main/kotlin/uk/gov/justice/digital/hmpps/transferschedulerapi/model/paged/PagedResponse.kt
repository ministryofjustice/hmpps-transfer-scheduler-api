package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

interface PagedResponse<T> {
  val content: List<T>
  val metadata: PageMetadata
}

data class PageMetadata(val totalElements: Long)
