package uk.gov.justice.digital.hmpps.transferschedulerapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfersRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfersResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.BulkScheduleTransfers

@Tag(name = OpenApiTags.UI)
@RestController
@RequestMapping("bulk/transfers")
@PreAuthorize("hasRole('${Roles.TRANSFER_SCHEDULER_UI}')")
class BulkController(private val bulk: BulkScheduleTransfers) {
  @CaseloadIdHeader
  @PutMapping
  fun bulkTransfers(@Valid @RequestBody request: BulkTransfersRequest): BulkTransfersResponse = bulk.merge(request)
}
