package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.RetrieveForSync
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.TransferSync
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("sync/transfers")
@PreAuthorize("hasRole('${Roles.TRANSFER_SYNC}')")
class SyncController(
  private val transfer: TransferSync,
  private val retrieve: RetrieveForSync,
) {
  @PutMapping("/{personIdentifier}")
  fun syncTransfer(
    @PathVariable personIdentifier: String,
    @Valid @RequestBody request: SyncTransferRequest,
  ): SyncTransferResponse = transfer.sync(personIdentifier, request)

  @GetMapping("/{id}")
  fun getTransfer(@PathVariable id: UUID): SyncTransfer = retrieve.transfer(id)

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteTransfer(@PathVariable id: UUID) = transfer.delete(id)
}
