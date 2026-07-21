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
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.COMPLETED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.IN_TRANSIT
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferCompleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementLogisticsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRecategorised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransferResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncUser
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.syncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst

class SyncTransferMovementIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYNC, personIdentifier())
      .bodyValue(syncTransferRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    sendTransfer(personIdentifier(), syncTransfer(movement = syncMovement()), syncUser(), Roles.TRANSFER_SCHEDULER_UI)
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if schedule and movement are missing`() {
    sendTransfer(personIdentifier(), syncTransfer(schedule = null, movement = null), syncUser())
      .errorResponse(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `200 - can create an unschedule transfer movement`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))

    val request = syncTransfer(schedule = null, movement = syncMovement(active = true))
    val user = syncUser()
    val res = sendTransfer(prisoner.prisonerNumber, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(IN_TRANSIT.name)
    assertThat(saved.stage).isEqualTo(TransferStage.UNSCHEDULED)
    saved verifyAgainst request
    assertThat(saved.plan).isNull()
    assertThat(saved.schedule).isNull()
    assertThat(saved.reason).isNull()
    assertThat(saved.logistics).isNull()
    assertThat(saved.destinationCode).isNull()

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferRecorded(prisoner.prisonerNumber, saved.id, DataSource.NOMIS).publication(saved.id),
        TransferMovementRecorded(prisoner.prisonerNumber, saved.id, DataSource.NOMIS).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can relocate an unscheduled transfer`() {
    val transfer = givenTransfer(
      transfer(
        statusCode = COMPLETED,
        reasonCode = null,
        logisticsCode = null,
        destinationCode = null,
        plan = null,
        schedule = null,
        movement = movement(),
      ),
    )
    val newDestination = prisonCode()

    val request =
      transfer.toTestSyncModel().copy(syncMovement = transfer.syncMovement()!!.copy(toAgyLocId = newDestination))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(COMPLETED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.UNSCHEDULED)
    saved verifyAgainst request
    assertThat(saved.plan).isNull()
    assertThat(saved.schedule).isNull()
    assertThat(saved.reason).isNull()
    assertThat(saved.logistics).isNull()
    assertThat(saved.destinationCode).isNull()

    verifyAudit(
      saved.movement!!,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved.movement!!,
      setOf(TransferMovementRelocated(transfer.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id)),
    )
  }

  @Test
  fun `200 - can recategorise an unscheduled transfer`() {
    val transfer = givenTransfer(
      transfer(
        statusCode = IN_TRANSIT,
        reasonCode = null,
        logisticsCode = null,
        destinationCode = null,
        plan = null,
        schedule = null,
        movement = movement(),
      ),
    )
    val newReason = generateSequence { TransferReasonCode.randomCode() }.first { it != transfer.movement!!.reason.code }

    val request = transfer.toTestSyncModel().copy(syncMovement = transfer.syncMovement()!!.copy(movementReasonCode = newReason, active = false))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(COMPLETED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.UNSCHEDULED)
    saved verifyAgainst request
    assertThat(saved.plan).isNull()
    assertThat(saved.schedule).isNull()
    assertThat(saved.reason).isNull()
    assertThat(saved.logistics).isNull()
    assertThat(saved.destinationCode).isNull()

    verifyAudit(
      saved.movement!!,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved.movement!!,
      setOf(
        TransferMovementRecategorised(
          transfer.person.identifier,
          saved.id,
          DataSource.NOMIS,
        ).publication(saved.id),
        TransferCompleted(
          transfer.person.identifier,
          saved.id,
          DataSource.NOMIS,
        ).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can change logistics for an unscheduled transfer`() {
    val transfer = givenTransfer(
      transfer(
        statusCode = COMPLETED,
        reasonCode = null,
        logisticsCode = null,
        destinationCode = null,
        plan = null,
        schedule = null,
        movement = movement(),
      ),
    )
    val newLogistics = generateSequence { TransferLogisticsCode.randomCode() }.first { it != transfer.movement!!.logistics.code }

    val request =
      transfer.toTestSyncModel().copy(syncMovement = transfer.syncMovement()!!.copy(escortCode = newLogistics))
    val user = syncUser()
    val res = sendTransfer(transfer.person.identifier, request, user).successResponse<SyncTransferResponse>()

    val saved = requireNotNull(findTransfer(res.dpsId))
    assertThat(saved.status.code).isEqualTo(COMPLETED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.UNSCHEDULED)
    saved verifyAgainst request
    assertThat(saved.plan).isNull()
    assertThat(saved.schedule).isNull()
    assertThat(saved.reason).isNull()
    assertThat(saved.logistics).isNull()
    assertThat(saved.destinationCode).isNull()

    verifyAudit(
      saved.movement!!,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Movement::class.simpleName!!),
      SchedulerContext.get()
        .copy(username = user.username, caseloadId = user.activeCaseloadId, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved.movement!!,
      setOf(
        TransferMovementLogisticsChanged(
          transfer.person.identifier,
          saved.id,
          DataSource.NOMIS,
        ).publication(saved.id),
      ),
    )
  }

  private fun sendTransfer(
    personIdentifier: String,
    request: SyncTransfer,
    syncUser: SyncUser,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC, personIdentifier)
    .bodyValue(syncTransferRequest(request, syncUser))
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC = "/sync/transfers/{personIdentifier}"
  }
}
