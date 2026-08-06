package uk.gov.justice.digital.hmpps.transferschedulerapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags.INTEGRATIONS
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.IntegrationResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.RetrieveTransfer
import java.util.UUID

@Tag(name = INTEGRATIONS)
@RestController
@RequestMapping("/integrations/transfers")
@PreAuthorize("hasAnyRole('${Roles.TRANSFER_RO}', '${Roles.TRANSFER_RW}')")
class IntegrationController(val retrieve: RetrieveTransfer) {
  @GetMapping("/{id}")
  fun getTransferForIntegration(@PathVariable id: UUID): IntegrationResponse = retrieve.forIntegration(id)
}
