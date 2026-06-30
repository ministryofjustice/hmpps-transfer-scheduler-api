package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.getTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrieveTransfer(
  private val transferRepository: TransferRepository,
  private val prisonRegister: PrisonRegisterClient,
) {
  fun byId(id: UUID): Transfer {
    val transfer = transferRepository.getTransfer(id)
    val prisons = prisonRegister.prisonProvider(setOfNotNull(transfer.prisonCode, transfer.destinationCode))
    return transfer.asModel(prisons::get)
  }
}
