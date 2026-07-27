package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferPriorityCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.PlanTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ScheduleTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncWaitlist
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDate
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
  fun `404 - not found if unscheduled transfer id provided`() {
    val transfer = givenTransfer(
      transfer(
        statusCode = TransferStatus.Code.IN_TRANSIT,
        movement = movement(),
        schedule = null,
        plan = null,
      ),
    )
    retrieveTransfer(transfer.id).errorResponse(HttpStatus.NOT_FOUND)
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

  @Test
  fun `approved user matches user moving to scheduled`() {
    SchedulerContext.get()
      .copy(
        username = DEFAULT_USERNAME,
        requestAt = LocalDate.now().minusDays(1).atTime(12, 0),
      ).set()
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE))

    val approvedUsername = username()
    val updated = transactionTemplate.execute {
      SchedulerContext.get().copy(username = approvedUsername, requestAt = LocalDate.now().atTime(8, 0)).set()
      requireNotNull(findTransfer(transfer.id))
        .applySchedule(ScheduleTransfer(transfer.schedule!!.start, word(12)), rdRepository.rdProvider())
    }

    val res = retrieveTransfer(transfer.id).successResponse<SyncTransfer>()
    updated verifyAgainst res
    assertThat(res.syncWaitlist?.statusDate).isEqualTo(LocalDate.now())
    assertThat(res.syncWaitlist?.approved).isTrue
    assertThat(res.syncWaitlist?.approvedUsername).isEqualTo(approvedUsername)
    assertThat(res.syncWaitlist?.waitListStatus).isEqualTo(SyncWaitlist.CONFIRMED)
  }

  @Test
  fun `status date matches latest date switching to plan`() {
    SchedulerContext.get()
      .copy(
        username = DEFAULT_USERNAME,
        requestAt = LocalDate.now().minusDays(2).atTime(12, 0),
      ).set()
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.READY_TO_SCHEDULE))

    transactionTemplate.executeWithoutResult {
      SchedulerContext.get()
        .copy(username = username(), requestAt = LocalDate.now().minusDays(1).atTime(8, 0)).set()
      requireNotNull(findTransfer(transfer.id))
        .applySchedule(ScheduleTransfer(transfer.schedule!!.start, word(12)), rdRepository.rdProvider())
    }

    val pt = PlanTransfer(LocalDate.now().minusDays(1), TransferPriorityCode.randomCode(), word(12))
    val updated = transactionTemplate.execute {
      SchedulerContext.get().copy(username = username(), requestAt = LocalDate.now().atTime(8, 0)).set()
      requireNotNull(findTransfer(transfer.id)).applyPlan(pt, rdRepository.rdProvider())
    }

    val res = retrieveTransfer(transfer.id).successResponse<SyncTransfer>()
    updated verifyAgainst res
    assertThat(res.syncWaitlist?.statusDate).isEqualTo(LocalDate.now())
    assertThat(res.syncWaitlist?.approved).isFalse
    assertThat(res.syncWaitlist?.approvedUsername).isNull()
    assertThat(res.syncWaitlist?.waitListStatus).isEqualTo(SyncWaitlist.PENDING)
  }

  @Test
  fun `cancelled plan is correctly returned`() {
    val transfer = givenTransfer(transfer(statusCode = TransferStatus.Code.CANCELLED, stage = TransferStage.PLANNING))

    val res = retrieveTransfer(transfer.id).successResponse<SyncTransfer>()
    transfer verifyAgainst res
    assertThat(res.syncWaitlist?.waitListStatus).isEqualTo(SyncWaitlist.CANCELLED)
    assertThat(res.syncWaitlist?.outcomeReasonCode).isEqualTo(SyncWaitlist.OutcomeReasonCode.ADMI)
    assertThat(res.syncSchedule.eventStatus).isEqualTo(SyncSchedule.CANCELLED)
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
