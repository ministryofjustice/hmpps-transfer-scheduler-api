package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ReconciliationResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst

class SyncReconciliationRetrievalIntTest(
  @Autowired personOps: PersonSummaryOperations,
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  PersonSummaryOperations by personOps,
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(RETRIEVE_RECONCILIATION_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    retrieveTransfers(personIdentifier(), role = Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `200 ok - empty lists when no transfers found`() {
    val res = retrieveTransfers(personIdentifier()).successResponse<ReconciliationResponse>()
    assertThat(res.transfers).isEmpty()
    assertThat(res.unscheduledMovements).isEmpty()
  }

  @Test
  fun `200 - can retrieve details of transfer`() {
    val person = givenPersonSummary(personSummary())
    val planned = givenTransfer(transfer(person.identifier, schedule = null, statusCode = TransferStatus.Code.PLANNING))
    val scheduled = givenTransfer(transfer(person.identifier, plan = null, statusCode = TransferStatus.Code.SCHEDULED))
    val schMov = givenTransfer(transfer(person.identifier, movement = movement(), statusCode = TransferStatus.Code.IN_TRANSIT))
    val unMov = givenTransfer(transfer(person.identifier, schedule = null, plan = null, movement = movement(), statusCode = TransferStatus.Code.COMPLETED))

    val res = retrieveTransfers(person.identifier).successResponse<ReconciliationResponse>()
    assertThat(res.transfers).hasSize(3)
    assertThat(res.unscheduledMovements).hasSize(1)

    planned verifyAgainst res.transfers.first { it.transfer.dpsId == planned.id }.transfer
    scheduled verifyAgainst res.transfers.first { it.transfer.dpsId == scheduled.id }.transfer
    val schMovRes = res.transfers.first { it.transfer.dpsId == schMov.id }
    schMov verifyAgainst schMovRes.transfer
    schMov.movement!! verifyAgainst schMovRes.movement!!
    unMov.movement!! verifyAgainst res.unscheduledMovements.first { it.dpsId == unMov.movement!!.id }
  }

  private fun retrieveTransfers(
    personIdentifier: String,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(RETRIEVE_RECONCILIATION_URL, personIdentifier)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RETRIEVE_RECONCILIATION_URL = "/reconciliation/transfers/{personIdentifier}"
  }
}
