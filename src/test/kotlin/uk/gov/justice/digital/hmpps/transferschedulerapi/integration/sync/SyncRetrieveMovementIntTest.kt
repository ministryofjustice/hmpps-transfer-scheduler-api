package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.util.UUID

class SyncRetrieveMovementIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(RETRIEVE_MOVEMENT_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    retrieveMovement(newUuid(), role = Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `404 - not found if uuid does not exist`() {
    retrieveMovement(newUuid()).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `404 - not found if unscheduled transfer id provided`() {
    val transfer = givenTransfer(
      transfer(
        statusCode = TransferStatus.Code.IN_TRANSIT,
        movement = movement(),
        schedule = null,
        plan = null,
      ),
    )
    retrieveMovement(transfer.id).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `400 - bad request if id not a valid uuid`() {
    retrieveMovement("invalid-uuid").errorResponse(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `200 - can retrieve scheduled movement`() {
    val transfer = givenTransfer(transfer(movement = movement()))
    val movement = transfer.movement!!
    val res = retrieveMovement(movement.id).successResponse<SyncMovement>()
    movement verifyAgainst res
    assertThat(res.dpsTransferId).isEqualTo(transfer.id)
  }

  @Test
  fun `200 - can retrieve unscheduled movement`() {
    val transfer = givenTransfer(
      transfer(
        statusCode = TransferStatus.Code.COMPLETED,
        movement = movement(),
        schedule = null,
        plan = null,
      ),
    )
    val movement = transfer.movement!!
    val res = retrieveMovement(movement.id).successResponse<SyncMovement>()
    movement verifyAgainst res
    assertThat(res.dpsTransferId).isNull()
  }

  private fun retrieveMovement(
    id: UUID,
    role: String? = Roles.TRANSFER_SYNC,
  ) = retrieveMovement(id.toString(), role)

  private fun retrieveMovement(
    id: String,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .get()
    .uri(RETRIEVE_MOVEMENT_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RETRIEVE_MOVEMENT_URL = "/sync/transfer-movements/{id}"
  }
}
