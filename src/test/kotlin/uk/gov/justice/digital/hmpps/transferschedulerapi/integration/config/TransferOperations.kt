package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import java.util.UUID

interface TransferOperations {
  fun findTransfer(uuid: UUID): Transfer?
}

class TransferOperationsImpl(private val transferRepository: TransferRepository) : TransferOperations {
  override fun findTransfer(uuid: UUID): Transfer? = transferRepository.findByIdOrNull(uuid)
}
