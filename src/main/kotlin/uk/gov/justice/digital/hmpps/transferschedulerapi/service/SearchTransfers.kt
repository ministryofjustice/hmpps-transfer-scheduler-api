package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.destinationCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.logisticsCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.startsBetween
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPersonName
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPersonPrisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPrisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferReasonCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferStatusCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.StageRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferPrisonSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchResponse

@Service
class SearchTransfers(
  private val transferRepository: TransferRepository,
  private val prisonRegister: PrisonRegisterClient,
) {
  fun findForPrison(prisonCode: String, request: TransferPrisonSearchRequest): TransferSearchResponse = transferRepository.findAll(request.asSpecification(prisonCode), request.pageable()).asSearchResponse()

  private fun TransferSearchRequest.defaults(): List<Specification<Transfer>> = listOfNotNull(
    statusCodeOverrides().takeIf { it.isNotEmpty() }?.let { transferStatusCodeIn(it) },
    reasonCodes.takeIf { it.isNotEmpty() }?.let { transferReasonCodeIn(it) },
  )

  private fun TransferSearchRequest.statusCodeOverrides(): Set<TransferStatus.Code> = if (this is StageRequest) {
    fun Collection<TransferStatus.Code>.filtered() = filter { it !in (INVALID_STATUSES[stage] ?: emptySet()) }.toSet()
    statusCodes.filtered().takeIf { it.isNotEmpty() } ?: TransferStatus.Code.entries.filtered().toSet()
  } else {
    statusCodes
  }

  private fun TransferPrisonSearchRequest.asSpecification(prisonCode: String): Specification<Transfer> = (
    listOfNotNull(
      transferMatchesPrisonCode(prisonCode),
      startsBetween(start, end, stage),
      destinationCodes.takeIf { it.isNotEmpty() }?.let { destinationCodeIn(it) },
      logisticsCodes.takeIf { it.isNotEmpty() }?.let { logisticsCodeIn(it) },
      query?.let {
        if (it.isPersonIdentifier()) {
          transferMatchesPersonIdentifier(it, prisonCode)
        } else {
          transferMatchesPersonName(it, prisonCode)
        }
      } ?: transferMatchesPersonPrisonCode(prisonCode),
    ) + defaults()
    ).reduce(Specification<Transfer>::and)

  private fun String.isPersonIdentifier(): Boolean = matches(Prisoner.PATTERN.toRegex())

  private fun Page<Transfer>.asSearchResponse(): TransferSearchResponse {
    val prisonCodes: Set<String> = map { listOfNotNull(it.prisonCode, it.destinationCode) }.flatten().toSet()
    val prisons = prisonRegister.prisonProvider(prisonCodes)
    return map { item -> item.asModel(prisons::get) }.asResponse()
  }

  private fun Page<uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer>.asResponse() = TransferSearchResponse(content, PageMetadata(totalElements))

  companion object {
    private val INVALID_STATUSES: Map<TransferStage, Set<TransferStatus.Code>> = mapOf(
      TransferStage.SCHEDULED to setOf(
        TransferStatus.Code.PLANNING,
        TransferStatus.Code.READY_TO_SCHEDULE,
      ),
      TransferStage.PLANNING to setOf(
        TransferStatus.Code.SCHEDULED,
        TransferStatus.Code.IN_TRANSIT,
        TransferStatus.Code.COMPLETED,
      ),
    )
  }
}
