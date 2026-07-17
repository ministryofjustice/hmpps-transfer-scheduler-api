package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.getTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import java.util.UUID

@Service
class RetrieveForSync(private val transferRepository: TransferRepository) {
  fun transfer(id: UUID): SyncTransfer = transferRepository.getTransfer(id).toSyncModel()
}
