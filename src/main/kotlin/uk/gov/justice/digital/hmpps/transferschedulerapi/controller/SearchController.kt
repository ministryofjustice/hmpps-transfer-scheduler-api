package uk.gov.justice.digital.hmpps.transferschedulerapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags.UI
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferPrisonSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.SearchTransfers

@Tag(name = UI)
@RestController
@RequestMapping("/search")
@PreAuthorize("hasRole('${Roles.TRANSFER_SCHEDULER_UI}')")
class SearchController(
  private val transfers: SearchTransfers,
) {
  @PostMapping("/prisons/{prisonCode}/transfers")
  fun findTransfersForPrison(
    @PathVariable prisonCode: String,
    @Valid @RequestBody request: TransferPrisonSearchRequest,
  ): TransferSearchResponse = transfers.findForPrison(prisonCode, request)
}
