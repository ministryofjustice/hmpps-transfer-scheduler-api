package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.RetrieveForSync

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("reconciliation/transfers")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class ReconciliationController(private val retrieve: RetrieveForSync) {
  @GetMapping("/{personIdentifier}")
  fun reconcile(@PathVariable personIdentifier: String): ReconciliationResponse = retrieve.all(personIdentifier)
}

data class ReconciliationResponse(val transfers: List<SyncTransfer>, val unscheduledMovements: List<SyncMovement>)
