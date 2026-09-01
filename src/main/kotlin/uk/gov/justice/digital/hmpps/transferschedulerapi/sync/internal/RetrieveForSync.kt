package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.history.StatusChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.history.TransferHistoryService
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ReconciliationResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ReconciliationTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrieveForSync(
  private val transferRepository: TransferRepository,
  private val movementRepository: MovementRepository,
  private val transferHistoryService: TransferHistoryService,
  private val msa: MigrationSystemAuditRepository,
) {
  fun transfer(id: UUID): SyncTransfer {
    val transfer = transferRepository.findByIdOrNull(id)
      ?.takeIf { it.stage != TransferStage.UNSCHEDULED }
    val legacyData = msa.findByIdOrNull(id)?.data
    return transfer?.toSyncModel(transferHistoryService::getStatusChanges) { _ -> legacyData }
      ?: throw NotFoundException("Transfer not found")
  }

  fun movement(id: UUID): SyncMovement = movementRepository.findByIdOrNull(id)
    ?.syncMovement() ?: throw NotFoundException("Movement not found")

  fun all(personIdentifier: String): ReconciliationResponse {
    val all = transferRepository.findAllByPersonIdentifier(personIdentifier)
    val legacyData = msa.findAllById(all.map { it.id }).associateBy { it.id }
    val mapped = all.mapNotNull { tr -> tr.forReconciliation(transferHistoryService::getStatusChanges) { legacyData[it]?.data } }
    return ReconciliationResponse(
      mapped.filterIsInstance<ReconciliationTransfer>(),
      mapped.filterIsInstance<SyncMovement>(),
    )
  }
}

private fun Transfer.forReconciliation(
  statusChanges: (UUID) -> List<StatusChanged>,
  legacyDataProvider: (UUID) -> LegacyData?,
): Any? = when (stage) {
  TransferStage.UNSCHEDULED -> movement?.syncMovement()
  else -> ReconciliationTransfer(toSyncModel(statusChanges, legacyDataProvider), movement?.syncMovement())
}
