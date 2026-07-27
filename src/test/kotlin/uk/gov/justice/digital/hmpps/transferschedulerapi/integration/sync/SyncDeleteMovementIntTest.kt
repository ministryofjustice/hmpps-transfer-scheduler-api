package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferCompleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferDeleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementDeleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import java.util.UUID

class SyncDeleteMovementIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .delete()
      .uri(DELETE_MOVEMENT_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    deleteMovement(newUuid(), role = Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `400 - bad request if id not a valid uuid`() {
    deleteMovement("invalid-uuid").errorResponse(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `204 no content if uuid does not exist`() {
    deleteMovement(newUuid()).expectStatus().isNoContent
  }

  @Test
  fun `204 no content - can delete a movement leaving schedule`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.IN_TRANSIT, movement = movement()))
    val movement = requireNotNull(transfer.movement)
    deleteMovement(movement.id).expectStatus().isNoContent

    val updated = requireNotNull(findTransfer(transfer.id))
    assertThat(updated.movement).isNull()
    assertThat(updated.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)

    verifyAudit(
      movement,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Movement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )
    verifyEventPublications(
      transfer,
      setOf(
        TransferCompleted(transfer.person.identifier, transfer.id, transfer.stage, DataSource.NOMIS).publication(transfer.id),
        TransferMovementDeleted(transfer.person.identifier, transfer.id, movement.id, DataSource.NOMIS).publication(movement.id),
      ),
    )
  }

  @Test
  fun `204 no content - can delete an unscheduled movement`() {
    val mv = movement()
    val transfer = givenTransfer(
      transfer(
        destinationCode = mv.destinationCode,
        reasonCode = mv.reasonCode,
        logisticsCode = mv.logisticsCode,
        statusCode = TransferStatus.Code.COMPLETED,
        movement = mv,
        schedule = null,
        plan = null,
      ),
    )
    val movement = requireNotNull(transfer.movement)

    deleteMovement(transfer.movement!!.id).expectStatus().isNoContent
    assertThat(findTransfer(transfer.id)).isNull()

    verifyAudit(
      transfer,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Movement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyAudit(
      movement,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Movement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      transfer,
      setOf(
        TransferDeleted(transfer.person.identifier, transfer.id, transfer.stage, DataSource.NOMIS).publication(transfer.id),
        TransferMovementDeleted(transfer.person.identifier, transfer.id, movement.id, DataSource.NOMIS).publication(movement.id),
      ),
    )
  }

  private fun deleteMovement(
    id: UUID,
    role: String? = Roles.TRANSFER_SYNC,
  ) = deleteMovement(id.toString(), role)

  private fun deleteMovement(
    id: String,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .delete()
    .uri(DELETE_MOVEMENT_URL, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val DELETE_MOVEMENT_URL = "/sync/transfer-movements/{id}"
  }
}
