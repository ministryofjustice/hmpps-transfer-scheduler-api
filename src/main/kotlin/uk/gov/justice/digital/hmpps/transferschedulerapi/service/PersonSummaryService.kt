package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.PrisonerSearchClient

@Service
@Transactional
class PersonSummaryService(
  private val prisonerSearch: PrisonerSearchClient,
  private val personSummaryRepository: PersonSummaryRepository,
) {
  fun findPersonSummary(personIdentifier: String): PersonSummary? = personSummaryRepository.findByIdOrNull(personIdentifier)

  fun getWithSave(personIdentifier: String): PersonSummary = findPersonSummary(personIdentifier)
    ?: (prisonerSearch.getPrisoner(personIdentifier)?.let { personSummaryRepository.save(it.summary()) })
    ?: throw NotFoundException("Prisoner not found")

  fun updateExistingDetails(prisonNumber: String) {
    personSummaryRepository.findByIdOrNull(prisonNumber)?.also {
      val prisoner = requireNotNull(prisonerSearch.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
      it.update(
        prisoner.firstName,
        prisoner.lastName,
        prisoner.lastPrisonId,
        prisoner.cellLocation,
      )
    }
  }

  fun remove(personSummary: PersonSummary) = personSummaryRepository.delete(personSummary)

  private fun Prisoner.summary() = PersonSummary(firstName, lastName, lastPrisonId, cellLocation, prisonerNumber)
}
