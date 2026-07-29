package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMigrated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovedToPlanning
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementMigrated
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.MovementOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.nullStateIsEqual
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.IncompletePlanHandler
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncTransfersRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.LegacyData
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.MigrationSystemAuditRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ResyncTransfersIntTest(
  @Autowired personOps: PersonSummaryOperations,
  @Autowired transferOps: TransferOperations,
  @Autowired movementOps: MovementOperations,
  @Autowired private val msaRepository: MigrationSystemAuditRepository,
) : IntegrationTestBase(),
  PersonSummaryOperations by personOps,
  TransferOperations by transferOps,
  MovementOperations by movementOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(RESYNC, personIdentifier())
      .bodyValue(resyncRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    sendTransfers(personIdentifier(), resyncRequest(), Roles.TRANSFER_SCHEDULER_UI).expectStatus().isForbidden
  }

  @Test
  fun `200 ok can migrate data`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    val request = resyncRequest(
      listOf(
        resyncTransfer(modified = AtAndBy(LocalDateTime.now(), username())),
        resyncTransfer(movement = resyncMovement()),
        resyncTransfer(
          transfer = syncTransfer(
            waitlist = syncWaitList(),
            schedule = syncSchedule(eventStatus = SyncSchedule.PENDING, hiddenCommentText = null),
          ),
        ),
      ),
      listOf(resyncMovement()),
    )

    val res = sendTransfers(prisoner.prisonerNumber, request).successResponse<ResyncResponse>()
    assertThat(res.transfers).hasSize(3)
    assertThat(res.unscheduledMovements).hasSize(1)

    res.transfers.forEach { tr ->
      val saved = requireNotNull(findTransfer(tr.dpsId))
      val detail = request.transfers.first { it.transfer.eventId == tr.eventId }
      saved verifyAgainst detail.transfer
      val msa = requireNotNull(msaRepository.findByIdOrNull(saved.id))
      assertThat(msa.createdBy).isEqualTo(detail.created.by)
      detail.modified?.also { assertThat(msa.modifiedBy).isEqualTo(it.by) }
      tr.movement?.also { sm ->
        val savedMov = requireNotNull(findMovement(sm.dpsId))
        val smDetail = detail.movement!!.also {
          with(it.movement) { offenderBookId == sm.bookingId && movementSeq == sm.sequenceNumber }
        }
        savedMov verifyAgainst smDetail.movement
      }
      verifyAudit(
        saved,
        RevisionType.ADD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          Transfer::class.simpleName!!,
          Plan::class.simpleName!!,
          Schedule::class.simpleName!!,
          Movement::class.simpleName!!,
        ),
        SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }
    res.unscheduledMovements.forEach { um ->
      val mov = requireNotNull(findMovement(um.dpsId))
      val detail = request.unscheduledMovements.first {
        with(it.movement) { offenderBookId == um.bookingId && movementSeq == um.sequenceNumber }
      }
      mov verifyAgainst detail.movement
      assertThat(mov.transfer.stage).isEqualTo(TransferStage.UNSCHEDULED)
    }

    val trs = res.transfers.mapNotNull { findTransfer(it.dpsId) }
    val mvs = res.unscheduledMovements.mapNotNull { findMovement(it.dpsId) }
    assertThat(trs).hasSize(3)
    assertThat(mvs).hasSize(1)
    verifyEventPublications(
      trs.first(),
      trs.flatMap { tr ->
        setOfNotNull(
          TransferMigrated(tr.person.identifier, tr.id, tr.stage, DataSource.NOMIS)
            .publication(tr.id) { false },
          tr.movement?.let { mv ->
            TransferMovementMigrated(tr.person.identifier, tr.id, mv.id, DataSource.NOMIS)
              .publication(mv.id) { false }
          },
        )
      }.toSet() +
        mvs.flatMap { mv ->
          setOf(
            TransferMigrated(mv.transfer.person.identifier, mv.transfer.id, mv.transfer.stage, DataSource.NOMIS)
              .publication(mv.transfer.id) { false },
            TransferMovementMigrated(mv.transfer.person.identifier, mv.transfer.id, mv.id, DataSource.NOMIS)
              .publication(mv.id) { false },
          )
        }.toSet(),
    )
  }

  @Test
  fun `200 ok can remove data`() {
    val person = givenPersonSummary(personSummary())
    val schTr = givenTransfer(transfer(person.identifier))
    val movTr = givenTransfer(transfer(person.identifier, movement = movement()))
    val unschMov = givenTransfer(transfer(person.identifier, schedule = null, plan = null, movement = movement()))

    val request = resyncRequest(listOf(), listOf())

    val res = sendTransfers(person.identifier, request).successResponse<ResyncResponse>()
    assertThat(res.transfers).isEmpty()
    assertThat(res.unscheduledMovements).isEmpty()

    assertThat(findPersonSummary(person.identifier)).isNull()

    verifyAudit(
      schTr,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Plan::class.simpleName!!,
        Schedule::class.simpleName!!,
        Movement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyAudit(
      movTr,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Plan::class.simpleName!!,
        Schedule::class.simpleName!!,
        Movement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyAudit(
      unschMov,
      RevisionType.DEL,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Plan::class.simpleName!!,
        Schedule::class.simpleName!!,
        Movement::class.simpleName!!,
      ),
      SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )
  }

  @Test
  fun `200 ok can merge data`() {
    val person = givenPersonSummary(personSummary())
    val schTr = givenTransfer(transfer(person.identifier, legacyId = newId()))
    val movTr = givenTransfer(transfer(person.identifier, movement = movement()))
    val unschMov = givenTransfer(transfer(person.identifier, schedule = null, plan = null, movement = movement()))

    val request = resyncRequest(
      listOf(
        resyncTransfer(transfer = syncTransfer(schedule = syncSchedule(hiddenCommentText = null))),
        resyncTransfer(transfer = syncTransfer(eventId = requireNotNull(schTr.legacyId))),
        resyncTransfer(
          transfer = syncTransfer(dpsId = movTr.id, waitlist = syncWaitList()),
          movement = resyncMovement(movement = syncMovement(dpsId = movTr.movement!!.id)),
        ),
      ),
      listOf(resyncMovement(movement = syncMovement(dpsId = unschMov.movement!!.id))),
    )

    val newTransfer = request.transfers.first()

    val res = sendTransfers(person.identifier, request).successResponse<ResyncResponse>()
    assertThat(res.transfers).hasSize(3)
    assertThat(res.unscheduledMovements).hasSize(1)

    res.transfers.forEach { tr ->
      val saved = requireNotNull(findTransfer(tr.dpsId))
      val detail = request.transfers.first { it.transfer.eventId == tr.eventId }
      saved verifyAgainst detail.transfer
      val msa = requireNotNull(msaRepository.findByIdOrNull(saved.id))
      assertThat(msa.createdBy).isEqualTo(detail.created.by)
      detail.modified?.also { assertThat(msa.modifiedBy).isEqualTo(it.by) }
      msa.data verifyAgainst detail.transfer
      tr.movement?.also { sm ->
        val savedMov = requireNotNull(findMovement(sm.dpsId))
        val smDetail = detail.movement!!.also {
          with(it.movement) { offenderBookId == sm.bookingId && movementSeq == sm.sequenceNumber }
        }
        savedMov verifyAgainst smDetail.movement
      }
      verifyAudit(
        saved,
        if (saved.legacyId == newTransfer.transfer.eventId) RevisionType.ADD else RevisionType.MOD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          Transfer::class.simpleName!!,
          Plan::class.simpleName!!,
          Schedule::class.simpleName!!,
          Movement::class.simpleName!!,
        ),
        SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }
    res.unscheduledMovements.forEach { um ->
      val mov = requireNotNull(findMovement(um.dpsId))
      val detail = request.unscheduledMovements.first {
        with(it.movement) { offenderBookId == um.bookingId && movementSeq == um.sequenceNumber }
      }
      mov verifyAgainst detail.movement
      assertThat(mov.transfer.stage).isEqualTo(TransferStage.UNSCHEDULED)
      verifyAudit(
        mov,
        RevisionType.MOD,
        setOf(
          HmppsDomainEvent::class.simpleName!!,
          Transfer::class.simpleName!!,
          Plan::class.simpleName!!,
          Schedule::class.simpleName!!,
          Movement::class.simpleName!!,
        ),
        SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
      )
    }
  }

  @Test
  fun `Incomplete planned transfers are completed by DPS`() {
    val prisonCode = prisonCode()
    val prisoner = prisonerSearch.givenPrisoner(prisoner(prisonCode))
    val tr1 = resyncTransfer(transfer = syncTransfer(waitlist = null, schedule = syncSchedule(eventStatus = SyncSchedule.PENDING)))
    val tr2 = resyncTransfer(transfer = syncTransfer(waitlist = null, schedule = syncSchedule(start = null, eventStatus = SyncSchedule.PENDING)))
    val request = resyncRequest(listOf(tr1, tr2))

    val res = sendTransfers(prisoner.prisonerNumber, request).successResponse<ResyncResponse>()
    assertThat(res.transfers).hasSize(2)
    assertThat(res.unscheduledMovements).isEmpty()

    waitUntil { findTransfer(res.transfers.last().dpsId)?.plan != null }

    val transfers = res.transfers.map { findTransfer(it.dpsId) }

    val readyToSchedule = requireNotNull(transfers.first { it!!.legacyId == tr1.transfer.eventId })
    assertThat(readyToSchedule.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    assertThat(readyToSchedule.stage).isEqualTo(TransferStage.PLANNING)

    val planning = requireNotNull(transfers.first { it!!.legacyId == tr2.transfer.eventId })
    assertThat(planning.status.code).isEqualTo(TransferStatus.Code.PLANNING.name)
    assertThat(planning.stage).isEqualTo(TransferStage.PLANNING)

    verifyAudit(
      readyToSchedule.plan!!,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Transfer::class.simpleName!!,
        Plan::class.simpleName!!,
      ),
      SchedulerContext.get().copy(reason = IncompletePlanHandler.REASON),
    )

    verifyAudit(
      planning.plan!!,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        Plan::class.simpleName!!,
      ),
      SchedulerContext.get().copy(reason = IncompletePlanHandler.REASON),
    )

    verifyEventPublications(
      readyToSchedule.plan!!,
      setOf(
        TransferMovedToPlanning(prisoner.prisonerNumber, readyToSchedule.id, readyToSchedule.stage)
          .publication(readyToSchedule.id),
      ),
    )

    verifyEventPublications(
      planning.plan!!,
      setOf(
        TransferMovedToPlanning(prisoner.prisonerNumber, planning.id, planning.stage)
          .publication(planning.id),
      ),
    )
  }

  private infix fun LegacyData?.verifyAgainst(request: SyncTransfer) {
    check(nullStateIsEqual(this?.waitList, request.syncWaitlist))
    if (request.syncSchedule.hiddenCommentText != null || request.syncSchedule.outcomeReasonCode != null) {
      checkNotNull(this?.schedule)
    } else {
      check(this?.schedule == null)
    }
    if (this?.waitList != null) {
      with(this.waitList) {
        assertThat(statusDate).isEqualTo(request.syncWaitlist?.statusDate)
        assertThat(approved).isEqualTo(request.syncWaitlist?.approved)
        assertThat(approvedStaffId).isEqualTo(request.syncWaitlist?.approvedUsername)
        assertThat(outcomeReasonCode).isEqualTo(request.syncWaitlist?.cancellationReason)
      }
    }
    if (this?.schedule != null) {
      with(this.schedule) {
        assertThat(hiddenCommentText).isEqualTo(request.syncSchedule.hiddenCommentText)
        assertThat(outcomeReasonCode).isEqualTo(request.syncSchedule.outcomeReasonCode)
      }
    }
  }

  private fun resyncRequest(
    transfers: List<ResyncTransfer> = listOf(),
    unscheduledMovements: List<ResyncMovement> = listOf(),
  ) = ResyncTransfersRequest(transfers, unscheduledMovements)

  private fun resyncTransfer(
    transfer: SyncTransfer = syncTransfer(),
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS), username()),
    modified: AtAndBy? = null,
    movement: ResyncMovement? = null,
  ) = ResyncTransfer(transfer, created, modified, movement)

  private fun resyncMovement(
    movement: SyncMovement = syncMovement(),
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS), username()),
    modified: AtAndBy? = null,
  ) = ResyncMovement(movement, created, modified)

  private fun sendTransfers(
    personIdentifier: String,
    request: ResyncTransfersRequest,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(RESYNC, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RESYNC = "/resync/transfers/{personIdentifier}"
  }
}
