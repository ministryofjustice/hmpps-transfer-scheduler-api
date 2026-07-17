package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.util.UUID

class SyncRetrieveTransferIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(RETRIEVE_TRANSFER_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    retrieveTransfer(newUuid(), role = Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `404 - not found if uuid does not exist`() {
    retrieveTransfer(newUuid()).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `400 - bad request if id not a valid uuid`() {
    retrieveTransfer("invalid-uuid").errorResponse(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `200 - can retrieve details of transfer`() {
    val transfer = givenTransfer(transfer())
    val res = retrieveTransfer(transfer.id).successResponse<SyncTransfer>()
    transfer verifyAgainst res
  }

  private fun retrieveTransfer(
    id: UUID,
    role: String? = Roles.TRANSFER_SYNC,
  ) = retrieveTransfer(id.toString(), role)

  private fun retrieveTransfer(
    id: String,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .get()
    .uri(RETRIEVE_TRANSFER_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RETRIEVE_TRANSFER_URL = "/sync/transfers/{id}"
  }
}
