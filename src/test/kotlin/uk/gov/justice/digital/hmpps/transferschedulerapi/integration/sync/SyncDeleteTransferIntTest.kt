package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferDeleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementDeleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import java.util.UUID

class SyncDeleteTransferIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .delete()
      .uri(DELETE_TRANSFER_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    deleteTransfer(newUuid(), role = Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `400 - bad request if id not a valid uuid`() {
    deleteTransfer("invalid-uuid").errorResponse(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `204 no content if uuid does not exist`() {
    deleteTransfer(newUuid()).expectStatus().isNoContent
  }

  @Test
  fun `204 no content - can delete a transfer`() {
    val transfer = givenTransfer(transfer())
    deleteTransfer(transfer.id).expectStatus().isNoContent
    assertThat(findTransfer(transfer.id)).isNull()

    verifyAudit(
      transfer,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Schedule::class.simpleName!!,
        Plan::class.simpleName!!,
      ),
      SchedulerContext.get().copy(source = DataSource.NOMIS),
    )
    verifyEventPublications(
      transfer,
      setOf(
        TransferDeleted(transfer.person.identifier, transfer.id, DataSource.NOMIS).publication(transfer.id),
      ),
    )
  }

  @Test
  fun `204 no content - can delete a transfer with movement`() {
    val transfer = givenTransfer(transfer(movement = movement()))
    deleteTransfer(transfer.id).expectStatus().isNoContent
    assertThat(findTransfer(transfer.id)).isNull()

    verifyAudit(
      transfer,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Schedule::class.simpleName!!,
        Plan::class.simpleName!!,
        Movement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(source = DataSource.NOMIS),
    )
    verifyEventPublications(
      transfer,
      setOf(
        TransferDeleted(transfer.person.identifier, transfer.id, DataSource.NOMIS).publication(transfer.id),
        TransferMovementDeleted(transfer.person.identifier, transfer.id, DataSource.NOMIS).publication(transfer.id),
      ),
    )
  }

  @Test
  fun `204 no content - can delete unscheduled transfer movement`() {
    val transfer = givenTransfer(
      transfer(
        reasonCode = null,
        destinationCode = null,
        logisticsCode = null,
        plan = null,
        schedule = null,
        statusCode = TransferStatus.Code.COMPLETED,
        stage = TransferStage.UNSCHEDULED,
        movement = movement(),
      ),
    )
    deleteTransfer(transfer.id).expectStatus().isNoContent
    assertThat(findTransfer(transfer.id)).isNull()

    verifyAudit(
      transfer,
      RevisionType.DEL,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get().copy(source = DataSource.NOMIS),
    )
    verifyEventPublications(
      transfer,
      setOf(
        TransferDeleted(transfer.person.identifier, transfer.id, DataSource.NOMIS).publication(transfer.id),
        TransferMovementDeleted(transfer.person.identifier, transfer.id, DataSource.NOMIS).publication(transfer.id),
      ),
    )
  }

  private fun deleteTransfer(
    id: UUID,
    role: String? = Roles.TRANSFER_SYNC,
  ) = deleteTransfer(id.toString(), role)

  private fun deleteTransfer(
    id: String,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .delete()
    .uri(DELETE_TRANSFER_URL, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val DELETE_TRANSFER_URL = "/sync/transfers/{id}"
  }
}
