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
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrieveForSync(
  private val transferRepository: TransferRepository,
  private val movementRepository: MovementRepository,
  private val transferHistoryService: TransferHistoryService,
) {
  fun transfer(id: UUID): SyncTransfer = transferRepository.findByIdOrNull(id)
    ?.takeIf { it.stage != TransferStage.UNSCHEDULED }
    ?.toSyncModel(transferHistoryService::getStatusChanges)
    ?: throw NotFoundException("Transfer not found")

  fun movement(id: UUID): SyncMovement = movementRepository.findByIdOrNull(id)
    ?.syncMovement() ?: throw NotFoundException("Movement not found")

  fun all(personIdentifier: String): ReconciliationResponse {
    val all = transferRepository.findAllByPersonIdentifier(personIdentifier)
      .mapNotNull { it.forReconciliation(transferHistoryService::getStatusChanges) }
    return ReconciliationResponse(all.filterIsInstance<SyncTransfer>(), all.filterIsInstance<SyncMovement>())
  }
}

private fun Transfer.forReconciliation(statusChanges: (UUID) -> List<StatusChanged>): SyncRequest? = when (stage) {
  TransferStage.UNSCHEDULED -> movement?.syncMovement()
  else -> toSyncModel(statusChanges)
}
