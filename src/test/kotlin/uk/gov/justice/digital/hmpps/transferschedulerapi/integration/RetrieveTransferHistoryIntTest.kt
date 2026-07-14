package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMigrated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferPlanned
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecategorised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.ManageUsersExtension.Companion.manageUsers
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyReason
import java.util.UUID

class RetrieveTransferHistoryIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(HISTORY_TRANSFER_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    retrieveHistory(newUuid(), role = "ROLE_ANY__OTHER").expectStatus().isForbidden
  }

  @Test
  fun `404 - not found if uuid does not exist`() {
    retrieveHistory(newUuid()).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `400 - bad request if id not a valid uuid`() {
    retrieveHistory("invalid-uuid").errorResponse(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `200 ok can retrieve initial history of migrated data`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    SchedulerContext.get().copy(username = SYSTEM_USERNAME, migratingData = true, source = DataSource.NOMIS).set()
    val transfer = givenTransfer(
      transfer(
        prisonCode = prison.code,
        destinationCode = destination.code,
        movement = movement(),
        statusCode = TransferStatus.Code.COMPLETED,
      ),
    )

    SchedulerContext.clear()

    val history = retrieveHistory(transfer.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(1)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User("SYS", "User SYS"))
      assertThat(domainEvents).containsExactly(TransferMigrated.EVENT_TYPE)
      assertThat(changes).isEmpty()
    }
  }

  @Test
  fun `200 ok can retrieve initial history of plan and schedule`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val initialUser = manageUsers.givenUser()
    SchedulerContext.get().copy(username = initialUser.username).set()
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code))

    SchedulerContext.clear()

    val history = retrieveHistory(transfer.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(1)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(initialUser.username, initialUser.name))
      assertThat(domainEvents).containsExactly(TransferScheduled.EVENT_TYPE)
      assertThat(changes).isEmpty()
    }
  }

  @Test
  fun `200 ok can retrieve initial history of planned transfer`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val initialUser = manageUsers.givenUser()
    SchedulerContext.get().copy(username = initialUser.username).set()
    val transfer = givenTransfer(
      transfer(
        prisonCode = prison.code,
        destinationCode = destination.code,
        schedule = null,
        statusCode = TransferStatus.Code.READY_TO_SCHEDULE,
      ),
    )

    SchedulerContext.clear()

    val history = retrieveHistory(transfer.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(1)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(initialUser.username, initialUser.name))
      assertThat(domainEvents).containsExactly(TransferPlanned.EVENT_TYPE)
      assertThat(changes).isEmpty()
    }
  }

  @Test
  fun `200 ok can retrieve initial history of directly scheduled transfer`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val initialUser = manageUsers.givenUser()
    SchedulerContext.get().copy(username = initialUser.username).set()
    val transfer = givenTransfer(
      transfer(
        prisonCode = prison.code,
        destinationCode = destination.code,
        plan = null,
        statusCode = TransferStatus.Code.SCHEDULED,
      ),
    )

    SchedulerContext.clear()

    val history = retrieveHistory(transfer.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(1)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(initialUser.username, initialUser.name))
      assertThat(domainEvents).containsExactly(TransferScheduled.EVENT_TYPE)
      assertThat(changes).isEmpty()
    }
  }

  @Test
  fun `200 ok can retrieve entire history of transfer`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val initialUser = manageUsers.givenUser()
    SchedulerContext.get().copy(username = initialUser.username).set()
    val rdProvider = rdRepository.rdProvider()
    val transfer = givenTransfer(
      transfer(
        prisonCode = prison.code,
        schedule = null,
        reasonCode = "PRES",
        statusCode = TransferStatus.Code.PLANNING,
      ),
    )

    val applyReason = ApplyReason("SEC")
    val reasonUser = manageUsers.givenUser()
    val reasonAuditReason = word(26)
    transactionTemplate.execute {
      SchedulerContext.get().copy(username = reasonUser.username, reason = reasonAuditReason).set()
      requireNotNull(findTransfer(transfer.id)).applyReason(applyReason, rdProvider)
    }

    val applyDestination = ApplyDestination(prisonCode())
    val destUser = manageUsers.givenUser()
    val destReason = word(26)
    transactionTemplate.execute {
      SchedulerContext.get().copy(username = destUser.username, reason = destReason).set()
      requireNotNull(findTransfer(transfer.id)).applyDestination(applyDestination)
    }

    SchedulerContext.clear()

    val history = retrieveHistory(transfer.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(3)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(initialUser.username, initialUser.name))
      assertThat(domainEvents).containsExactly(TransferPlanned.EVENT_TYPE)
      assertThat(reason).isNull()
      assertThat(changes).isEmpty()
    }
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(reasonUser.username, reasonUser.name))
      assertThat(domainEvents).containsExactly(TransferRecategorised.EVENT_TYPE)
      assertThat(reason).isEqualTo(reasonAuditReason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          Transfer::reason.name,
          "Pre Release Employment Scheme",
          "Security Reasons",
        ),
      )
    }
    with(history.content[2]) {
      assertThat(user).isEqualTo(AuditedAction.User(destUser.username, destUser.name))
      assertThat(domainEvents).containsExactly(TransferRelocated.EVENT_TYPE)
      assertThat(reason).isEqualTo(destReason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          Transfer::destinationCode.name,
          transfer.destinationCode,
          applyDestination.destinationCode,
        ),
      )
    }
  }

  private fun retrieveHistory(
    id: UUID,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = retrieveHistory(id.toString(), role)

  private fun retrieveHistory(
    id: String,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .get()
    .uri(HISTORY_TRANSFER_URL, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val HISTORY_TRANSFER_URL = "/transfers/{id}/history"
  }
}
