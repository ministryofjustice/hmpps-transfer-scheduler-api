package uk.gov.justice.digital.hmpps.transferschedulerapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.CreateTransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.InitiateTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.RetrieveTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.history.TransferHistoryService
import java.util.UUID

@Tag(name = OpenApiTags.UI)
@RestController
@RequestMapping("transfers")
@PreAuthorize("hasRole('${Roles.TRANSFER_SCHEDULER_UI}')")
class TransferController(private val initiate: InitiateTransfer, private val retrieve: RetrieveTransfer, private val history: TransferHistoryService) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{personIdentifier}")
  fun initiateTransfer(@PathVariable personIdentifier: String, @Valid @RequestBody request: CreateTransferRequest): Transfer = initiate.transferFor(personIdentifier, request)

  @GetMapping("/{id}")
  fun retrieveTransfer(@PathVariable id: UUID): Transfer = retrieve.byId(id)

  @GetMapping("/{id}/history")
  fun retrieveHistory(@PathVariable id: UUID): AuditHistory = history.changes(id)
}
