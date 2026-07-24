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
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.IN_TRANSIT
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.PLANNING
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PlanCommentsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PlanRequestedOnChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferCancelled
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovedToPlanning
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferReprioritised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferPriorityCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyPlanComments
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyRequestedOn
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.CancelTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.PlanTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ScheduleTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.TransferAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.TransferActions
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.util.UUID

class PlanModificationsIntTest(
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
  fun `200 - can change the priority of a transfer`() {
    val transfer = givenTransfer(transfer(schedule = null, statusCode = READY_TO_SCHEDULE))
    assertThat(transfer.status.code).isEqualTo(READY_TO_SCHEDULE.name)
    val newPriority =
      generateSequence { TransferPriorityCode.randomCode() }.first { it != transfer.plan?.priority?.code }
    val action = ApplyPriority(newPriority)
    val username = username()
    val givenReason = word(20)

    val res = applyAction(transfer.id, action, givenReason, username).successResponse<AuditHistory>()
    val rdPriority = rdRepository.rdProvider().get<TransferPriority>(action.priorityCode)
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(TransferReprioritised.EVENT_TYPE)
      assertThat(reason).isEqualTo(givenReason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          Plan::priority.name,
          transfer.plan!!.priority.description,
          rdPriority.description,
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id)?.plan)
    assertThat(saved.priority.description).isEqualTo(rdPriority.description)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = givenReason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferReprioritised(saved.transfer.person.identifier, saved.id, saved.transfer.stage).publication(saved.id),
      ),
    )
  }

  @Test
  fun `409 - not in a state for ready to schedule`() {
    val transfer = givenTransfer(transfer(plan = null, logisticsCode = null, statusCode = PLANNING))
    val action = PlanTransfer(LocalDate.now(), "3", word(10))
    val username = username()
    val givenReason = word(10)

    val res = applyAction(transfer.id, action, givenReason, username).errorResponse(HttpStatus.CONFLICT)
    assertThat(res.status).isEqualTo(HttpStatus.CONFLICT.value())
    assertThat(res.userMessage).isEqualTo("A conflict has been detected")

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(PLANNING.name)
    assertThat(saved.plan).isNull()
    assertThat(saved.logistics).isNull()

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!),
    )
  }

  @Test
  fun `409 - cannot revert to planning`() {
    val transfer = givenTransfer(transfer(plan = null, statusCode = IN_TRANSIT, movement = movement()))
    val action = PlanTransfer(LocalDate.now(), "3", word(10))
    val username = username()
    val givenReason = word(10)

    val res = applyAction(transfer.id, action, givenReason, username).errorResponse(HttpStatus.CONFLICT)
    assertThat(res.status).isEqualTo(HttpStatus.CONFLICT.value())
    assertThat(res.userMessage).isEqualTo("A conflict has been detected")

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(IN_TRANSIT.name)
    assertThat(saved.plan).isNull()

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!, Movement::class.simpleName!!),
    )
  }

  @Test
  fun `200 - can move a scheduled transfer to planning`() {
    val transfer = givenTransfer(transfer(plan = null))
    val action = PlanTransfer(LocalDate.now(), "3", word(10))
    val username = username()
    val givenReason = word(10)

    val res = applyAction(transfer.id, action, givenReason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(TransferMovedToPlanning.EVENT_TYPE)
      assertThat(reason).isEqualTo(givenReason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          Transfer::status.name,
          "Scheduled",
          "Ready to schedule",
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    assertThat(saved.plan).isNotNull
    saved.plan!! verifyAgainst action

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = givenReason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferMovedToPlanning(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can revert a scheduled transfer to planning`() {
    val transfer = givenTransfer(transfer(plan = plan(LocalDate.now().minusDays(1), "2")))
    val action = PlanTransfer(LocalDate.now(), "1", word(10))
    val username = username()
    val givenReason = word(10)

    val res = applyAction(transfer.id, action, givenReason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactlyInAnyOrder(
        TransferMovedToPlanning.EVENT_TYPE,
        TransferReprioritised.EVENT_TYPE,
        PlanCommentsChanged.EVENT_TYPE,
        PlanRequestedOnChanged.EVENT_TYPE,
      )
      assertThat(reason).isEqualTo(givenReason)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change(
          Transfer::status.name,
          "Scheduled",
          "Ready to schedule",
        ),
        AuditedAction.Change(
          propertyName = "requestedOn",
          previous = ISO_LOCAL_DATE.format(LocalDate.now().minusDays(1)),
          change = ISO_LOCAL_DATE.format(LocalDate.now()),
        ),
        AuditedAction.Change(propertyName = "priority", previous = "Medium", change = "High"),
        AuditedAction.Change(propertyName = "comments", previous = transfer.plan!!.comments, change = action.comments),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    assertThat(saved.plan).isNotNull
    saved.plan!! verifyAgainst action

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = givenReason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferMovedToPlanning(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
        TransferReprioritised(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
        PlanCommentsChanged(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
        PlanRequestedOnChanged(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can schedule a planned transfer`() {
    val transfer = givenTransfer(transfer(schedule = null, statusCode = READY_TO_SCHEDULE))
    val action = ScheduleTransfer(LocalDateTime.now().plusDays(7), word(20))
    val username = username()
    val givenReason = word(10)

    val res = applyAction(transfer.id, action, givenReason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(TransferScheduled.EVENT_TYPE)
      assertThat(reason).isEqualTo(givenReason)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change(
          Transfer::status.name,
          "Ready to schedule",
          "Scheduled",
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(SCHEDULED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.SCHEDULED)
    assertThat(saved.schedule).isNotNull
    saved.schedule!! verifyAgainst action

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = givenReason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferScheduled(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can update requested date on a planned transfer`() {
    val transfer = givenTransfer(transfer(schedule = null, statusCode = READY_TO_SCHEDULE))
    val action = ApplyRequestedOn(LocalDate.now().minusDays(7))

    val res = applyAction(transfer.id, action, reason = null).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(PlanRequestedOnChanged.EVENT_TYPE)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change(
          Plan::requestedOn.name,
          ISO_LOCAL_DATE.format(transfer.plan?.requestedOn),
          ISO_LOCAL_DATE.format(action.requestedOn),
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    assertThat(saved.plan?.requestedOn).isEqualTo(action.requestedOn)

    verifyAudit(
      saved.plan!!,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get().copy(username = DEFAULT_USERNAME),
    )

    verifyEventPublications(
      saved.plan!!,
      setOf(
        PlanRequestedOnChanged(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can update comments on a planned transfer`() {
    val transfer = givenTransfer(transfer(schedule = null, statusCode = READY_TO_SCHEDULE))
    val action = ApplyPlanComments(word(20))

    val res = applyAction(transfer.id, action, reason = null).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(PlanCommentsChanged.EVENT_TYPE)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change(
          Plan::comments.name,
          transfer.plan?.comments,
          action.comments,
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(READY_TO_SCHEDULE.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)
    assertThat(saved.plan?.comments).isEqualTo(action.comments)

    verifyAudit(
      saved.plan!!,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Plan::class.simpleName!!),
      SchedulerContext.get().copy(username = DEFAULT_USERNAME),
    )

    verifyEventPublications(
      saved.plan!!,
      setOf(
        PlanCommentsChanged(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 - can cancel a planned transfer`() {
    val transfer = givenTransfer(transfer(schedule = null, statusCode = READY_TO_SCHEDULE))
    val action = CancelTransfer
    val username = username()
    val givenReason = word(20)

    val res = applyAction(transfer.id, action, givenReason, username).successResponse<AuditHistory>()
    with(res.content.single()) {
      assertThat(domainEvents).containsExactly(TransferCancelled.EVENT_TYPE)
      assertThat(reason).isEqualTo(givenReason)
      assertThat(changes).containsExactly(
        AuditedAction.Change(
          Transfer::status.name,
          "Ready to schedule",
          "Cancelled",
        ),
      )
    }

    val saved = requireNotNull(findTransfer(transfer.id))
    assertThat(saved.status.code).isEqualTo(TransferStatus.Code.CANCELLED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.PLANNING)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!),
      SchedulerContext.get().copy(username = username, reason = givenReason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferCancelled(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
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
