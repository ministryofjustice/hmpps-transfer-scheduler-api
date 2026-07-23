package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.TransfersMove

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("move/transfers")
@PreAuthorize("hasRole('${Roles.TRANSFER_SYNC}')")
class MoveTransferController(private val transfers: TransfersMove) {
  @PutMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun moveTransfers(@RequestBody request: MoveTransfersRequest) = transfers.move(request)
}
