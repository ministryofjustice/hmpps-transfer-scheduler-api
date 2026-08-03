package uk.gov.justice.digital.hmpps.transferschedulerapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags.INTEGRATIONS
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.RetrieveClashes

@Tag(name = INTEGRATIONS)
@RestController
@RequestMapping("/search/people/clashes")
@PreAuthorize("hasRole('${Roles.SCHEDULE_CLASHES_RO}')")
class ClashController(private val clashes: RetrieveClashes) {
  @PostMapping
  fun findTapClashes(@Valid @RequestBody request: ClashRequest): ClashResponse = clashes.retrieve(request)
}
