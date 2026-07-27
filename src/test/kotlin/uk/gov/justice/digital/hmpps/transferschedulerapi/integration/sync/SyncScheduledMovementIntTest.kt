package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferCompleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferInTransit
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementCommentsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementLogisticsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementOccurredAtChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRecategorised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.MovementOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ReferenceId
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncUser
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SyncScheduledMovementIntTest(
  @Autowired transferOps: TransferOperations,
  @Autowired moveOps: MovementOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps,
  MovementOperations by moveOps {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYNC, personIdentifier())
      .bodyValue(syncMovementRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    sendMovement(personIdentifier(), syncMovement(), syncUser(), Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `200 - can create a new active scheduled movement`() {
    val transfer = givenTransfer(transfer())

    val request = transfer.movementSync(active = true)
    val user = syncUser()
    val res = sendMovement(transfer.person.identifier, request, user).successResponse<ReferenceId>()

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.IN_TRANSIT.name)
    assertThat(saved.stage == TransferStage.SCHEDULED)
    val movement = saved.movement!!
    movement verifyAgainst request
    assertThat(res.dpsId).isEqualTo(movement.id)

    verifyAudit(
      movement,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferInTransit(
          transfer.person.identifier,
          transfer.id,
          stage = transfer.stage,
          DataSource.NOMIS,
        ).publication(transfer.id),
        TransferMovementRecorded(
          transfer.person.identifier,
          transfer.id,
          movement.id,
          DataSource.NOMIS,
        ).publication(movement.id),
      ),
    )
  }

  @Test
  fun `200 - can create a new inactive scheduled movement`() {
    val transfer = givenTransfer(transfer())

    val request = transfer.movementSync(active = false)
    val user = syncUser()
    val res = sendMovement(transfer.person.identifier, request, user).successResponse<ReferenceId>()

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(saved.stage == TransferStage.SCHEDULED)
    val movement = saved.movement!!
    movement verifyAgainst request
    assertThat(res.dpsId).isEqualTo(movement.id)

    verifyAudit(
      movement,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferCompleted(
          transfer.person.identifier,
          transfer.id,
          stage = transfer.stage,
          DataSource.NOMIS,
        ).publication(transfer.id),
        TransferMovementRecorded(
          transfer.person.identifier,
          transfer.id,
          movement.id,
          DataSource.NOMIS,
        ).publication(movement.id),
      ),
    )
  }

  @Test
  fun `200 - can relocate a scheduled movement`() {
    val existing = givenTransfer(transfer(movement = movement(), statusCode = TransferStatus.Code.COMPLETED))
    val newDestination = prisonCode()

    val request = existing.movementSync().copy(active = false, toAgyLocId = newDestination)
    val user = syncUser()
    val res = sendMovement(existing.person.identifier, request, user).successResponse<ReferenceId>()

    val movement = requireNotNull(findMovement(res.dpsId))
    val transfer = movement.transfer
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(transfer.stage).isEqualTo(TransferStage.SCHEDULED)
    movement verifyAgainst request

    verifyAudit(
      movement,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferMovementRelocated(
          transfer.person.identifier,
          transfer.id,
          movement.id,
          DataSource.NOMIS,
        ).publication(movement.id),
      ),
    )
  }

  @Test
  fun `200 - can recategorise a scheduled transfer and update comments and occurred at`() {
    val existing = givenTransfer(transfer(movement = movement(), statusCode = TransferStatus.Code.IN_TRANSIT))
    val newReason = generateSequence { TransferReasonCode.randomCode() }.first { it != existing.movement!!.reason.code }

    val request = existing.movementSync(active = true).copy(
      movementReasonCode = newReason,
      comments = word(26),
      occurredAt = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS),
    )
    val user = syncUser()
    val res = sendMovement(existing.person.identifier, request, user).successResponse<ReferenceId>()

    val movement = requireNotNull(findMovement(res.dpsId))
    val transfer = movement.transfer
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.IN_TRANSIT.name)
    assertThat(transfer.stage).isEqualTo(TransferStage.SCHEDULED)
    movement verifyAgainst request

    verifyAudit(
      movement,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferMovementRecategorised(
          transfer.person.identifier,
          transfer.id,
          movement.id,
          DataSource.NOMIS,
        ).publication(movement.id),
        TransferMovementCommentsChanged(
          transfer.person.identifier,
          transfer.id,
          movement.id,
          DataSource.NOMIS,
        ).publication(movement.id),
        TransferMovementOccurredAtChanged(
          transfer.person.identifier,
          transfer.id,
          movement.id,
          DataSource.NOMIS,
        ).publication(movement.id),
      ),
    )
  }

  @Test
  fun `200 - can change scheduled transfer logistics`() {
    val existing = givenTransfer(transfer(movement = movement(), statusCode = TransferStatus.Code.COMPLETED))
    val newLogistics =
      generateSequence { TransferLogisticsCode.randomCode() }.first { it != existing.movement?.logistics?.code }

    val request = existing.movementSync(active = false).copy(escortCode = newLogistics)
    val user = syncUser()
    val res = sendMovement(existing.person.identifier, request, user).successResponse<ReferenceId>()

    val movement = requireNotNull(findMovement(res.dpsId))
    val transfer = movement.transfer
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(transfer.stage).isEqualTo(TransferStage.SCHEDULED)
    movement verifyAgainst request

    verifyAudit(
      movement,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferMovementLogisticsChanged(
          existing.person.identifier,
          transfer.id,
          movement.id,
          DataSource.NOMIS,
        ).publication(movement.id),
      ),
    )
  }

  @Test
  fun `200 - can complete a scheduled transfer`() {
    val existing = givenTransfer(transfer(movement = movement(), statusCode = TransferStatus.Code.IN_TRANSIT))
    val request = existing.movementSync(active = false)
    val user = syncUser()
    val res = sendMovement(existing.person.identifier, request, user).successResponse<ReferenceId>()

    val movement = requireNotNull(findMovement(res.dpsId))
    val transfer = movement.transfer
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(transfer.stage).isEqualTo(TransferStage.SCHEDULED)
    movement verifyAgainst request

    verifyAudit(
      transfer,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferCompleted(existing.person.identifier, transfer.id, transfer.stage, DataSource.NOMIS)
          .publication(transfer.id),
      ),
    )
  }

  @Test
  fun `200 - can move movement to another transfer`() {
    val existing = givenTransfer(transfer(movement = movement(), statusCode = TransferStatus.Code.COMPLETED))
    val another = givenTransfer(transfer())
    val request = existing.movementSync(active = false).copy(dpsTransferId = another.id)
    val user = syncUser()
    val res = sendMovement(existing.person.identifier, request, user).successResponse<ReferenceId>()

    val movement = requireNotNull(findMovement(res.dpsId))
    val transfer = movement.transfer
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(transfer.stage).isEqualTo(TransferStage.SCHEDULED)
    assertThat(transfer.id).isEqualTo(another.id)
    movement verifyAgainst request

    val noMovement = requireNotNull(findTransfer(existing.id))
    assertThat(noMovement.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(noMovement.stage).isEqualTo(TransferStage.SCHEDULED)
    assertThat(noMovement.movement).isNull()

    verifyAudit(
      movement,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferCompleted(transfer.person.identifier, transfer.id, transfer.stage, DataSource.NOMIS)
          .publication(transfer.id),
      ),
    )
  }

  @Test
  fun `200 - can make a movement unscheduled`() {
    val existing = givenTransfer(transfer(movement = movement(), statusCode = TransferStatus.Code.COMPLETED))
    val request = existing.movementSync(active = false).copy(dpsTransferId = null)
    val user = syncUser()
    val res = sendMovement(existing.person.identifier, request, user).successResponse<ReferenceId>()

    val movement = requireNotNull(findMovement(res.dpsId))
    val transfer = movement.transfer
    assertThat(transfer.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(transfer.stage).isEqualTo(TransferStage.UNSCHEDULED)
    movement verifyAgainst request

    val noMovement = requireNotNull(findTransfer(existing.id))
    assertThat(noMovement.status.code).isEqualTo(TransferStatus.Code.COMPLETED.name)
    assertThat(noMovement.stage).isEqualTo(TransferStage.SCHEDULED)
    assertThat(noMovement.movement).isNull()

    verifyAudit(
      movement,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      movement,
      setOf(
        TransferRecorded(transfer.person.identifier, transfer.id, transfer.stage, DataSource.NOMIS)
          .publication(transfer.id),
      ),
    )
  }

  private fun sendMovement(
    personIdentifier: String,
    request: SyncMovement,
    syncUser: SyncUser,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC, personIdentifier)
    .bodyValue(syncMovementRequest(request, syncUser))
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC = "/sync/transfer-movements/{personIdentifier}"
  }
}
