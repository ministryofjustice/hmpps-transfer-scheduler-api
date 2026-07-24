package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.history.TransferHistoryService
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrieveForSync(
  private val transferRepository: TransferRepository,
  private val transferHistoryService: TransferHistoryService,
) {
  fun transfer(id: UUID): SyncTransfer = transferRepository.findByIdOrNull(id)
    ?.takeIf { it.stage != TransferStage.UNSCHEDULED }
    ?.toSyncModel(transferHistoryService::getStatusChanges)
    ?: throw NotFoundException("Transfer not found")
}
