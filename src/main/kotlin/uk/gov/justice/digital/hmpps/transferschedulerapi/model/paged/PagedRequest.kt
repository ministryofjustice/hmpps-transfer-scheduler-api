package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

interface PagedRequest {
  @get:Parameter(description = "The page to request, starting at 1", example = "1")
  @get:Min(value = 1, message = "Page number must be at least 1")
  val page: Int

  @get:Parameter(description = "The page size to request", example = "10")
  @get:Min(value = 1, message = "Page size must be at least 1")
  val size: Int
  val sort: String

  fun validSortFields(): Set<String> = setOf()

  fun sort(): Sort {
    val validate: (String) -> String = {
      if (it in validSortFields()) it else throw IllegalArgumentException("Invalid sort field provided")
    }
    val split = sort.split(",")
    val (field, direction) = when (split.size) {
      1 -> validate(split[0]) to Sort.Direction.DESC
      else -> validate(split[0]) to if (split[1].lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
    }
    return buildSort(field, direction)
  }

  fun buildSort(field: String, direction: Sort.Direction): Sort = Sort.by(direction, field)

  fun pageable(): Pageable = PageRequest.of(page - 1, size, sort())
}
