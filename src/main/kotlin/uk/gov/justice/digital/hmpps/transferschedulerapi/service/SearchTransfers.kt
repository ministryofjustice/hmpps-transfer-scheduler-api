package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.destinationCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.logisticsCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.matchesStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.priorityCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.requestedOnBetween
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.startsBetween
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPrisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferPersonIdentifierIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferReasonCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferStatusCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.PlanningSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.PrisonTransferSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.QueryRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchResponse

@Service
class SearchTransfers(
  private val transferRepository: TransferRepository,
  private val prisonRegister: PrisonRegisterClient,
  private val prisonerSearch: PrisonerSearchClient,
) {
  fun findForPrison(prisonCode: String, request: PrisonTransferSearchRequest): TransferSearchResponse {
    val prisoners = prisonerSearch.findMatchingPrisoners(prisonCode, if (request is QueryRequest) request.query else null)
    return transferRepository.findAll(
      request.asSpecification(prisonCode, prisoners.personIdentifiers()),
      request.pageable(),
    ).asSearchResponse { requireNotNull(prisoners[it]) }
  }

  private fun TransferSearchRequest.defaults(): List<Specification<Transfer>> = listOfNotNull(
    statusCodes.takeIf { it.isNotEmpty() }?.let { transferStatusCodeIn(it) },
    reasonCodes.takeIf { it.isNotEmpty() }?.let { transferReasonCodeIn(it) },
  )

  private fun PrisonTransferSearchRequest.asSpecification(
    prisonCode: String,
    personIdentifiers: Set<String>,
  ): Specification<Transfer> = (
    listOfNotNull(
      transferMatchesPrisonCode(prisonCode),
      transferPersonIdentifierIn(personIdentifiers),
      matchesStage(stage),
      if (this is PlanningSearchRequest && isRequestedOnSearch()) requestedOnBetween(start!!, end!!) else startsBetween(start, end),
      destinationCodes.takeIf { it.isNotEmpty() }?.let { destinationCodeIn(it) },
      logisticsCodes.takeIf { it.isNotEmpty() }?.let { logisticsCodeIn(it) },
      if (this is PlanningSearchRequest) priorityCodes.takeIf { it.isNotEmpty() }?.let { priorityCodeIn(it) } else null,
    ) + defaults()
    ).reduce(Specification<Transfer>::and)

  private fun Page<Transfer>.asSearchResponse(prisonerProvider: (String) -> Prisoner): TransferSearchResponse {
    val prisonCodes: Set<String> = map { listOfNotNull(it.prisonCode, it.destinationCode) }.flatten().toSet()
    val prisons = prisonRegister.prisonProvider(prisonCodes)
    return map { item -> item.asModel(prisons::get, prisonerProvider) }.asResponse()
  }

  private fun Page<uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer>.asResponse() = TransferSearchResponse(content, PageMetadata(totalElements))
}
