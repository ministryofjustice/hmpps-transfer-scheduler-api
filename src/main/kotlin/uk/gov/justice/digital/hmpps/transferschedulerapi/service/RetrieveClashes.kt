package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.clashesFor
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.Clash
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashOrigin
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashPersonIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.PersonClashes

@Transactional(readOnly = true)
@Service
class RetrieveClashes(private val transferRepository: TransferRepository) {
  fun retrieve(request: ClashRequest): ClashResponse = transferRepository.findAll(
    clashesFor(request.personIdentifiers.map { it.value }.toSet(), request.ranges),
  ).groupBy { it.person.identifier }
    .map { (k, v) -> PersonClashes(k.asPersonIdentifier(), v.map { it.clash() }) }
    .let { ClashResponse(ORIGIN, it) }

  private fun String.asPersonIdentifier() = ClashPersonIdentifier(ClashPersonIdentifier.Type.PRISON_NUMBER, this)

  private fun Transfer.clash() = with(schedule!!) {
    Clash(start, start.withHour(23).withMinute(59), description(), OUTSIDE, additionalInfo())
  }

  private fun Transfer.description() = Clash.Description(reason.description, "Transfer")
  private fun Transfer.additionalInfo() = Clash.AdditionalInformation(prisonCode)

  companion object {
    const val PRODUCT_ID = "DPS138"
    val ORIGIN: ClashOrigin = ClashOrigin(ClashSource(PRODUCT_ID, "Schedule a transfer"))
    val OUTSIDE = Clash.Location("Outside")
  }
}
