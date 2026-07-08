package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.startsOnOrAfter
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.startsOnOrBefore
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPersonName
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPersonPrisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferMatchesPrisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferReasonCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.transferStatusCodeIn
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Prison
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
    statusCodes.takeIf { it.isNotEmpty() }?.let { transferStatusCodeIn(it) },
    reasonCodes.takeIf { it.isNotEmpty() }?.let { transferReasonCodeIn(it) },
  )

  private fun String.isPersonIdentifier(): Boolean = matches(Prisoner.PATTERN.toRegex())

  private fun TransferPrisonSearchRequest.asSpecification(prisonCode: String): Specification<Transfer> = (
    listOfNotNull(
      transferMatchesPrisonCode(prisonCode),
      startsOnOrAfter(start),
      startsOnOrBefore(end),
      query?.let {
        if (it.isPersonIdentifier()) {
          transferMatchesPersonIdentifier(it, prisonCode)
        } else {
          transferMatchesPersonName(it, prisonCode)
        }
      } ?: transferMatchesPersonPrisonCode(prisonCode),
    ) + defaults()
    ).reduce(Specification<Transfer>::and)

  private fun Page<Transfer>.asSearchResponse(): TransferSearchResponse {
    val prisons = prisonRegister.findPrisons(map { it.prisonCode }.toSet()).block()!!.associateBy { it.code }
    return map { item -> item.asModel { prisons[it] ?: Prison.default(it) } }.asResponse()
  }

  private fun Page<uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer>.asResponse() = TransferSearchResponse(content, PageMetadata(totalElements))
}
