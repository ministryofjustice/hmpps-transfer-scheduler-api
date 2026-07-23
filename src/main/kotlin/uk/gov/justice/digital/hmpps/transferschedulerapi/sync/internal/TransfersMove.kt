package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.PersonSummaryService
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.MoveTransfersRequest

@Transactional
@Service
class TransfersMove(
  private val personSummaryService: PersonSummaryService,
  private val transferRepository: TransferRepository,
) {
  fun move(request: MoveTransfersRequest) {
    SchedulerContext.get().copy(username = SYSTEM_USERNAME, reason = "Prisoner booking moved").set()
    val person = personSummaryService.getWithSave(request.to)
    transferRepository.findAllById(request.transferIds).forEach {
      it.movePerson(person, it.prisonCode)
    }
    if (transferRepository.countAllByPersonIdentifier(request.from) == 0) {
      personSummaryService.findPersonSummary(request.from)?.also(personSummaryService::remove)
    }
  }
}
