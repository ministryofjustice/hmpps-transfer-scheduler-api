package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.CreateTransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer

@Transactional
@Service
class InitiateTransfer(
  private val prisonRegister: PrisonRegisterClient,
  private val personSummaryService: PersonSummaryService,
  private val rdRepository: ReferenceDataRepository,
  private val transferRepository: TransferRepository,
) {
  fun transferFor(personIdentifier: String, request: CreateTransferRequest): Transfer {
    val person = personSummaryService.getWithSave(personIdentifier)
    val prisonCodes = setOfNotNull(person.prisonCode, request.destinationCode)
    val prisons = prisonRegister.prisonProvider(prisonCodes)
    require(prisons.containsAll(prisonCodes)) { "Prison not recognised" }

    val rdProvider = rdRepository.rdProvider()
    return transferRepository.save(request.asEntity(person, rdProvider)).asModel(prisons::get)
  }
}
