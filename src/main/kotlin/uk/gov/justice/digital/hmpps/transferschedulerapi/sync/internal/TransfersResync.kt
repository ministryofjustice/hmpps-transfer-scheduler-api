package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.InternalEvents
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PlanningIncomplete
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.PersonSummaryService
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.asEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ResyncTransfersRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.TransferMapping
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.TransferMovementMapping
import java.util.UUID

@Transactional
@Service
class TransfersResync(
  private val rdRepository: ReferenceDataRepository,
  private val transferRepository: TransferRepository,
  private val movementRepository: MovementRepository,
  private val msa: MigrationSystemAuditRepository,
  private val personSummaryService: PersonSummaryService,
  private val aep: ApplicationEventPublisher,
) {
  fun all(personIdentifier: String, request: ResyncTransfersRequest): ResyncResponse {
    SchedulerContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS, migratingData = true).set()
    val person = personSummaryService.getWithSave(personIdentifier)
    val rdProvider = rdRepository.rdProvider()
    val (transferLegacyIds, transferIds) = request.transferIds()
    val (movementLegacyIds, movementIds) = request.movementIds()
    val existing = findAllTransfers(personIdentifier, transferIds, transferLegacyIds, movementIds, movementLegacyIds)
    val maProvider = MigrationAuditProvider(msa.findAllById(existing.flatMap { listOfNotNull(it.id, it.movement?.id) }))

    val transferProvider =
      { id: UUID?, legacyId: Long -> existing.firstOrNull { it.id == id || it.legacyId == legacyId } }
    val movementProvider =
      { id: UUID?, legacyId: String? -> existing.firstOrNull { it.movement?.id == id || it.movement?.legacyId == legacyId }?.movement }

    val scheduled = request.transfers.associate { it.resync(person, transferProvider, movementProvider, rdProvider, maProvider) }
    val unscheduled = request.unscheduledMovements.associate { it.resync(person, null, movementProvider, rdProvider, maProvider) }

    val toKeep = if (existing.isEmpty()) emptyList() else removeNotInResync(scheduled.keys, unscheduled.keys, existing)
    if (request.isEmpty() && toKeep.isEmpty()) {
      personSummaryService.remove(person)
    }

    val internalEvents = scheduled.values.filter { it.stage == TransferStage.PLANNING && it.plan == null }
      .map { PlanningIncomplete(it.person.identifier, it.id) }
    if (internalEvents.isNotEmpty()) {
      aep.publishEvent(InternalEvents(internalEvents))
    }

    return ResyncResponse(scheduled.keys, unscheduled.keys)
  }

  private fun findAllTransfers(
    personIdentifier: String,
    transferIds: Set<UUID>,
    legacyIds: Set<Long>,
    movementIds: Set<UUID>,
    movementLegacyIds: Set<String>,
  ): List<Transfer> {
    val mappedBy = transferRepository.findTransferIds(personIdentifier, legacyIds, movementIds, movementLegacyIds)
    return transferRepository.findAllById((transferIds + mappedBy).toSet())
  }

  private fun ResyncTransfer.resync(
    person: PersonSummary,
    transferProvider: (UUID?, Long) -> Transfer?,
    movementProvider: (UUID?, String) -> Movement?,
    rdProvider: RdProvider,
    maProvider: MigrationAuditProvider,
  ): Pair<TransferMapping, Transfer> {
    val tr = transferProvider(transfer.dpsId, requireNotNull(transfer.legacyId))
      ?.updateFrom(transfer, person, rdProvider)
      ?: transferRepository.save(transfer.asEntity(person, rdRepository.rdProvider()))
    val sm = movement?.resync(person, tr, movementProvider, rdProvider, maProvider)
    val prisonCodeDifferent: Boolean = movement?.movement?.fromAgyLocId?.let { it != tr.prisonCode } ?: false
    mergeMigrationAudit(tr.id, created, modified, maProvider, transfer.legacyData(prisonCodeDifferent))
    return TransferMapping(tr.id, requireNotNull(transfer.eventId), sm?.first) to tr
  }

  private fun ResyncMovement.resync(
    person: PersonSummary,
    transfer: Transfer?,
    movementProvider: (UUID?, String) -> Movement?,
    rdProvider: RdProvider,
    maProvider: MigrationAuditProvider,
  ): Pair<TransferMovementMapping, Movement> {
    val existing = movementProvider(movement.dpsId, requireNotNull(movement.legacyId))
    val wrapper = transfer
      ?: existing?.transfer.takeIf { it?.stage == TransferStage.UNSCHEDULED }
      ?: movement.unscheduledTransfer(person, rdProvider)
    val mov = existing?.updateFrom(movement, wrapper, rdProvider)
      ?: wrapper.addMovement(movement, rdProvider)
    mergeMigrationAudit(mov.id, created, modified, maProvider, null)
    return TransferMovementMapping(mov.id, requireNotNull(movement.offenderBookId), requireNotNull(movement.movementSeq)) to mov
  }

  private fun SyncMovement.unscheduledTransfer(person: PersonSummary, rdProvider: RdProvider): Transfer {
    val statusCode = if (active == true) TransferStatus.Code.IN_TRANSIT else TransferStatus.Code.COMPLETED
    return transferRepository.save(
      Transfer(
        person,
        fromAgyLocId,
        rdProvider.get(reasonCode),
        rdProvider.get(statusCode.name),
        destinationCode,
        rdProvider.get(logisticsCode),
        TransferStage.UNSCHEDULED,
        null,
      ),
    )
  }

  private fun removeNotInResync(
    scheduled: Set<TransferMapping>,
    unscheduled: Set<TransferMovementMapping>,
    transfers: List<Transfer>,
  ): List<Transfer> {
    val movementIds = (scheduled.mapNotNull { it.movement?.dpsId } + unscheduled.map { it.dpsId }).toSet()
    val transferIds = scheduled.map { it.dpsId }.toSet()
    val (movToKeep, movToDelete) = transfers.mapNotNull { it.movement }.partition { it.id in movementIds }
    val (trToKeep, trToDelete) = transfers.partition { it.id in transferIds || (it.stage == TransferStage.UNSCHEDULED && it.movement!!.id in movementIds) }
    val (unscheduledToDelete, scheduledToDelete) = movToDelete.partition { it.transfer.stage == TransferStage.UNSCHEDULED }
    movementRepository.deleteAll(scheduledToDelete + unscheduledToDelete)
    transferRepository.deleteAll(trToDelete + unscheduledToDelete.map { it.transfer })
    return trToKeep + movToKeep.map { it.transfer }
  }

  private fun mergeMigrationAudit(
    id: UUID,
    created: AtAndBy,
    modified: AtAndBy?,
    maProvider: MigrationAuditProvider,
    data: LegacyData?,
  ) {
    maProvider[id]?.also {
      it.createdBy = created.by
      it.createdAt = created.at
      it.modifiedBy = modified?.by
      it.modifiedAt = modified?.at
      it.data = data
    } ?: run {
      maProvider.put(
        msa.save(
          MigrationSystemAudit(id, created.at, created.by, modified?.at, modified?.by, data),
        ),
      )
    }
  }
}

private class MigrationAuditProvider(audits: Iterable<MigrationSystemAudit>) {
  private val audits = audits.associateBy { it.id }.toMutableMap()

  operator fun get(id: UUID): MigrationSystemAudit? = audits[id]

  fun put(msa: MigrationSystemAudit) {
    audits[msa.id] = msa
  }
}
