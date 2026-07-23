package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.MoveTransfersRequest
import java.util.SequencedSet
import java.util.UUID

class MoveTransferIntTest(
  @Autowired pso: PersonSummaryOperations,
  @Autowired tro: TransferOperations,
) : IntegrationTestBase(),
  PersonSummaryOperations by pso,
  TransferOperations by tro {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(MOVE_TRANSFER_URL, newUuid())
      .bodyValue(moveRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    moveTransfers(moveRequest(), role = Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `204 no content - can move transfers`() {
    val one = givenTransfer(transfer())
    val two = givenTransfer(transfer())
    val request = moveRequest(sortedSetOf(one.id, two.id), one.person.identifier, two.person.identifier)
    moveTransfers(request).expectStatus().isNoContent

    val moved = requireNotNull(findTransfer(one.id))
    assertThat(moved.person.identifier).isEqualTo(two.person.identifier)

    val retained = requireNotNull(findTransfer(two.id))
    assertThat(retained.person.identifier).isEqualTo(two.person.identifier)

    assertThat(findPersonSummary(one.person.identifier)).isNull()

    verifyAudit(
      moved,
      RevisionType.MOD,
      setOf(Transfer::class.simpleName!!),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, reason = "Prisoner booking moved", source = DataSource.NOMIS),
    )
  }

  private fun moveRequest(
    transferIds: SequencedSet<UUID> = sortedSetOf(),
    from: String = personIdentifier(),
    to: String = personIdentifier(),
  ) = MoveTransfersRequest(from, to, transferIds)

  private fun moveTransfers(
    request: MoveTransfersRequest,
    role: String? = Roles.TRANSFER_SYNC,
  ) = webTestClient
    .put()
    .uri(MOVE_TRANSFER_URL)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val MOVE_TRANSFER_URL = "/move/transfers"
  }
}
