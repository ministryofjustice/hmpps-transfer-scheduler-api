package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
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
) {
  fun transfer(id: UUID): SyncTransfer {
    val transfer = transferRepository.findByIdOrNull(id)
      ?.takeIf { it.stage != TransferStage.UNSCHEDULED }
    return transfer?.toSyncModel() ?: throw NotFoundException("Transfer not found")
  }

  fun movement(id: UUID): SyncMovement = movementRepository.findByIdOrNull(id)
    ?.syncMovement() ?: throw NotFoundException("Movement not found")

  fun all(personIdentifier: String): ReconciliationResponse {
    val all = transferRepository.findAllByPersonIdentifier(personIdentifier)
    val mapped = all.mapNotNull { tr -> tr.forReconciliation() }
    return ReconciliationResponse(
      mapped.filterIsInstance<ReconciliationTransfer>(),
      mapped.filterIsInstance<SyncMovement>(),
    )
  }
}

private fun Transfer.forReconciliation(): Any? = when (stage) {
  TransferStage.UNSCHEDULED -> movement?.syncMovement()
  else -> ReconciliationTransfer(toSyncModel(), movement?.syncMovement())
}
