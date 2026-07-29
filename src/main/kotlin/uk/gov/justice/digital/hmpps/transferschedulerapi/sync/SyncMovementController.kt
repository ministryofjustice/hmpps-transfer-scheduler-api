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
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.MovementSync
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.RetrieveForSync
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("sync/transfer-movements")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class SyncMovementController(
  private val movement: MovementSync,
  private val retrieve: RetrieveForSync,
) {
  @PutMapping("/{personIdentifier}")
  fun syncMovement(
    @PathVariable personIdentifier: String,
    @Valid @RequestBody request: SyncMovementRequest,
  ): ReferenceId = movement.sync(personIdentifier, request)

  @GetMapping("/{id}")
  fun getMovement(@PathVariable id: UUID): SyncMovement = retrieve.movement(id)

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteMovement(@PathVariable id: UUID) = movement.delete(id)
}
