package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.AuditContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.getTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.TransferActions
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.history.TransferHistoryService
import java.util.UUID

@Service
class TransferModifications(
  private val transaction: TransactionTemplate,
  private val rdRepository: ReferenceDataRepository,
  private val transferRepository: TransferRepository,
  private val history: TransferHistoryService,
) {
  fun apply(id: UUID, request: TransferActions): AuditHistory = try {
    newAuditContext()
    SchedulerContext.get().copy(reason = request.reason).set()
    transaction.execute {
      val transfer = transferRepository.getTransfer(id)
      val rdProvider = rdRepository.rdProvider()
      request.actions.forEach { action -> action.applyTo(transfer, rdProvider) }
    }
    return AuditContext.get()?.currentRevision?.id?.let {
      AuditHistory(listOfNotNull(history.changesForRevision(id, it)))
    } ?: AuditHistory(emptyList())
  } finally {
    clearAuditContext()
  }

  companion object {
    private var context = ThreadLocal<AuditContext>()

    internal fun getAuditContext(): AuditContext? = context.get()
    private fun newAuditContext() {
      context.set(AuditContext())
    }

    private fun clearAuditContext() {
      context.remove()
    }
  }
}
