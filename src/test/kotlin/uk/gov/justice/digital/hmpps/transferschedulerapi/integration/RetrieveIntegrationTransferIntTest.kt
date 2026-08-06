package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.IntegrationResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.nullStateIsEqual
import java.time.temporal.ChronoUnit
import java.util.UUID

class RetrieveIntegrationTransferIntTest(
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
  fun `200 - can retrieve transfer with plan, schedule and movement`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, movement = movement()))

    val res = retrieveTransfer(transfer.id).successResponse<IntegrationResponse>()
    res.data verifyAgainst transfer
  }

  @Test
  fun `200 - can retrieve transfer with plan and schedule`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, movement = null))
    assertThat(transfer.movement).isNull()

    val res = retrieveTransfer(transfer.id).successResponse<IntegrationResponse>()
    res.data verifyAgainst transfer
  }

  @Test
  fun `200 - can retrieve transfer with plan only`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, schedule = null, movement = null))
    assertThat(transfer.schedule).isNull()
    assertThat(transfer.movement).isNull()

    val res = retrieveTransfer(transfer.id).successResponse<IntegrationResponse>().data
    res verifyAgainst transfer
    assertThat(res.schedule).isNull()
  }

  @Test
  fun `200 - can retrieve transfer with schedule only`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, plan = null, movement = null))
    assertThat(transfer.plan).isNull()
    assertThat(transfer.movement).isNull()

    val res = retrieveTransfer(transfer.id).successResponse<IntegrationResponse>().data
    res verifyAgainst transfer
    assertThat(res.plan).isNull()
  }

  private fun retrieveTransfer(
    id: UUID,
    role: String? = listOf(Roles.TRANSFER_RO, Roles.TRANSFER_RW).random(),
  ) = retrieveTransfer(id.toString(), role)

  private fun retrieveTransfer(
    id: String,
    role: String? = listOf(Roles.TRANSFER_RO, Roles.TRANSFER_RW).random(),
  ) = webTestClient
    .get()
    .uri(RETRIEVE_TRANSFER_URL, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RETRIEVE_TRANSFER_URL = "/integrations/transfers/{id}"
  }
}

private infix fun IntegrationResponse.Transfer.verifyAgainst(transfer: uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer) {
  assertThat(id).isEqualTo(transfer.id)
  assertThat(personIdentifier).isEqualTo(transfer.person.identifier)
  assertThat(status.code).isEqualTo(transfer.status.code)
  assertThat(reason.code).isEqualTo(transfer.reason.code)
  assertThat(prisonCode).isEqualTo(transfer.prisonCode)
  assertThat(destinationCode).isEqualTo(transfer.destinationCode)
  assertThat(logistics?.code).isEqualTo(transfer.logistics?.code)
  check(nullStateIsEqual(plan, transfer.plan)) { "Invalid plan state" }
  check(nullStateIsEqual(schedule, transfer.schedule)) { "Invalid schedule state" }
  check(nullStateIsEqual(movement, transfer.movement)) { "Invalid movement state" }
  plan?.also { it verifyAgainst transfer.plan!! }
  schedule?.also { it verifyAgainst transfer.schedule!! }
  movement?.also { it verifyAgainst transfer.movement!! }
}

private infix fun IntegrationResponse.Plan.verifyAgainst(plan: uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan) {
  assertThat(requestedOn).isEqualTo(plan.requestedOn)
  assertThat(priority.code).isEqualTo(plan.priority.code)
  assertThat(comments).isEqualTo(plan.comments)
}

private infix fun IntegrationResponse.Schedule.verifyAgainst(schedule: uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule) {
  assertThat(start.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(schedule.start.truncatedTo(ChronoUnit.SECONDS))
  assertThat(comments).isEqualTo(schedule.comments)
}

private infix fun IntegrationResponse.Movement.verifyAgainst(movement: uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement) {
  assertThat(occurredAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(movement.occurredAt.truncatedTo(ChronoUnit.SECONDS))
  assertThat(comments).isEqualTo(movement.comments)
}
