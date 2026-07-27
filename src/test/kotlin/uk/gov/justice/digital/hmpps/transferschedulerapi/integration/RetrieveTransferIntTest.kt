package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.util.UUID

class RetrieveTransferIntTest(
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
    retrieveTransfer(newUuid(), role = "ROLE_ANY__OTHER").expectStatus().isForbidden
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
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code))

    val res = retrieveTransfer(transfer.id).successResponse<Transfer>()
    res verifyAgainst transfer
    assertThat(res.prison.name).isEqualTo(prison.name)
    assertThat(res.destination!!.name).isEqualTo(destination.name)
  }

  @Test
  fun `200 - can retrieve transfer with plan and schedule`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, movement = null))
    assertThat(transfer.movement).isNull()

    val res = retrieveTransfer(transfer.id).successResponse<Transfer>()
    res verifyAgainst transfer
    assertThat(res.prison.name).isEqualTo(prison.name)
    assertThat(res.destination!!.name).isEqualTo(destination.name)
  }

  @Test
  fun `200 - can retrieve transfer with plan only`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, schedule = null, movement = null))
    assertThat(transfer.schedule).isNull()
    assertThat(transfer.movement).isNull()

    val res = retrieveTransfer(transfer.id).successResponse<Transfer>()
    res verifyAgainst transfer
    assertThat(res.prison.name).isEqualTo(prison.name)
    assertThat(res.destination!!.name).isEqualTo(destination.name)
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

    val res = retrieveTransfer(transfer.id).successResponse<Transfer>()
    res verifyAgainst transfer
    assertThat(res.prison.name).isEqualTo(prison.name)
    assertThat(res.destination!!.name).isEqualTo(destination.name)
    assertThat(res.plan).isNull()
  }

  @Test
  fun `200 - can retrieve transfer with no subparts - invalid data from nomis`() {
    val prison = prison()
    val destination = prison()
    prisonRegister.givenPrisons(setOf(prison, destination))
    val transfer = givenTransfer(transfer(prisonCode = prison.code, destinationCode = destination.code, plan = null, schedule = null, movement = null))
    assertThat(transfer.plan).isNull()
    assertThat(transfer.schedule).isNull()
    assertThat(transfer.movement).isNull()

    val res = retrieveTransfer(transfer.id).successResponse<Transfer>()
    res verifyAgainst transfer
    assertThat(res.prison.name).isEqualTo(prison.name)
    assertThat(res.destination!!.name).isEqualTo(destination.name)
    assertThat(res.plan).isNull()
    assertThat(res.schedule).isNull()
  }

  private fun retrieveTransfer(
    id: UUID,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = retrieveTransfer(id.toString(), role)

  private fun retrieveTransfer(
    id: String,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .get()
    .uri(RETRIEVE_TRANSFER_URL, id)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RETRIEVE_TRANSFER_URL = "/transfers/{id}"
  }
}
