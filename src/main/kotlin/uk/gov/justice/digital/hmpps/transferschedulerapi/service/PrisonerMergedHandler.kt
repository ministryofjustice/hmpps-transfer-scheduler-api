package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PrisonerMerged
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.nomis.MigrationClient

@Service
class PrisonerMergedHandler(
  private val transaction: TransactionTemplate,
  private val transferRepository: TransferRepository,
  private val personSummaryService: PersonSummaryService,
  private val migrationClient: MigrationClient,
) {
  fun handle(de: PrisonerMerged) {
    val pmi = de.additionalInformation
    personSummaryService.findPersonSummary(pmi.removedNomsNumber)?.also { person ->
      SchedulerContext.get().copy(reason = PrisonerMerged.DESCRIPTION, source = DataSource.NOMIS).set()
      transaction.executeWithoutResult {
        val toPerson = personSummaryService.getWithSave(pmi.nomsNumber)
        transferRepository.findAllByPersonIdentifier(pmi.removedNomsNumber).forEach { transfer ->
          transfer.movePerson(toPerson)
        }
        personSummaryService.remove(person)
      }
      migrationClient.requestRepair(pmi.removedNomsNumber)
    }
    migrationClient.requestRepair(pmi.nomsNumber)
  }
}
