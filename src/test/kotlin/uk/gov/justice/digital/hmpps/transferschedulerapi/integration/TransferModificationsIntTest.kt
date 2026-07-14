package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecategorised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.ApplyReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.TransferAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.TransferActions
import java.util.UUID

class TransferModificationsIntTest(
  @Autowired tro: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by tro {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(TRANSFER_MODIFICATIONS_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    applyAction(newUuid(), ApplyDestination("ANY"), role = "ROLE_ANY__OTHER").expectStatus().isForbidden
  }

  @Test
  fun `404 - not found if uuid does not exist`() {
    applyAction(newUuid(), ApplyDestination("ANY")).errorResponse(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `200 - can change destination prison for a transfer`() {
    val destination = prison()
    val transfer = givenTransfer(transfer())
    val action = ApplyDestination(destination.code)
    val username = username()
    val givenReason = word(20)

    val res = applyAction(transfer.id, action, givenReason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(TransferRelocated.EVENT_TYPE)
      assertThat(reason).isEqualTo(givenReason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          Transfer::destinationCode.name,
          transfer.destinationCode,
          action.destinationCode,
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.destinationCode).isEqualTo(action.destinationCode)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = givenReason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferRelocated(saved.person.identifier, saved.id).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can change the reason for a transfer`() {
    val transfer = givenTransfer(transfer())
    val newReasonCode = generateSequence { TransferReasonCode.randomCode() }.first { it != transfer.reason.code }
    val action = ApplyReason(newReasonCode)
    val username = username()
    val givenReason = word(20)

    val res = applyAction(transfer.id, action, givenReason, username).successResponse<AuditHistory>()
    val rdReason = rdRepository.rdProvider().get<TransferReason>(action.reasonCode)
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(TransferRecategorised.EVENT_TYPE)
      assertThat(reason).isEqualTo(givenReason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          Transfer::reason.name,
          transfer.reason.description,
          rdReason.description,
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.reason.description).isEqualTo(rdReason.description)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = givenReason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferRecategorised(saved.person.identifier, saved.id).publication(saved.id),
      ),
    )
  }

  private fun applyAction(
    id: UUID,
    action: TransferAction,
    reason: String? = word(20),
    username: String = DEFAULT_USERNAME,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .put()
    .uri(TRANSFER_MODIFICATIONS_URL, id)
    .bodyValue(TransferActions(listOf(action), reason))
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val TRANSFER_MODIFICATIONS_URL = "/transfers/{id}"
  }
}
