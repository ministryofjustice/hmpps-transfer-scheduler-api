package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.MakeUnscheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.PersonSummaryService
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.asEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransferResponse
import java.util.UUID

@Transactional
@Service
class TransferSync(
  private val rdRepository: ReferenceDataRepository,
  private val personSummaryService: PersonSummaryService,
  private val transferRepository: TransferRepository,
) {
  fun sync(personIdentifier: String, request: SyncTransferRequest): SyncTransferResponse = with(request) {
    SchedulerContext.get().copy(
      requestAt = occurredAt,
      username = syncUser.username,
      caseloadId = syncUser.activeCaseloadId,
      reason = request.transfer.syncWaitlist?.cancellationReason,
    ).set()
    val person = personSummaryService.getWithSave(personIdentifier)
    val saved: Transfer = (
      transfer.dpsId?.let { transferRepository.findByIdOrNull(it) }
        ?: transfer.legacyId?.let { transferRepository.findByLegacyId(it) }
      )
      ?.updateFrom(transfer, person, rdRepository.rdProvider())
      ?: transferRepository.save(transfer.asEntity(person, rdRepository.rdProvider()))

    val legacyIdParts = saved.movement?.syncIdsFromLegacyId()
    SyncTransferResponse(saved.id, saved.legacyId, legacyIdParts?.first, legacyIdParts?.second)
  }

  fun delete(id: UUID) {
    transferRepository.findByIdOrNull(id)?.let { tr ->
      SchedulerContext.get().copy(username = SYSTEM_USERNAME).set()
      val rdProvider = rdRepository.rdProvider()
      if (tr.movement != null) {
        tr.makeUnscheduled(MakeUnscheduled, rdProvider)
      } else {
        transferRepository.delete(tr)
      }
    }
  }
}
