package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.getTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PlanningIncomplete
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.PlanTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.history.TransferHistoryService

@Transactional
@Service
class IncompletePlanHandler(
  private val rdRepository: ReferenceDataRepository,
  private val transferRepository: TransferRepository,
  private val transferHistoryService: TransferHistoryService,
) {
  fun handle(de: PlanningIncomplete) {
    SchedulerContext.get().copy(reason = REASON).set()
    transferRepository.getTransfer(de.additionalInformation.id).takeIf { it.plan == null && it.stage == TransferStage.PLANNING }?.also { tr ->
      val created = transferHistoryService.changes(tr.id).content.first()
      tr.applyPlan(
        PlanTransfer(created.occurredAt.toLocalDate(), TransferPriority.Code.LOW.value, REASON),
        rdRepository.rdProvider(),
        tr.logistics != null && tr.destinationCode != null && tr.schedule != null,
      )
    }
  }

  companion object {
    const val REASON = "Plan automatically created for part completed NOMIS transfer"
  }
}
