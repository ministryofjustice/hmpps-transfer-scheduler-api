package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.InternalEvents
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PlanningIncomplete
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.MakeUnscheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.PersonSummaryService
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.asEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ReferenceId
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransferRequest
import java.util.UUID

@Transactional
@Service
class TransferSync(
  private val rdRepository: ReferenceDataRepository,
  private val personSummaryService: PersonSummaryService,
  private val transferRepository: TransferRepository,
  private val aep: ApplicationEventPublisher,
) {
  fun sync(personIdentifier: String, request: SyncTransferRequest): ReferenceId = with(request) {
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

    if (saved.stage == TransferStage.PLANNING && saved.plan == null) {
      aep.publishEvent(InternalEvents(PlanningIncomplete(saved.person.identifier, saved.id)))
    }

    ReferenceId(saved.id)
  }

  fun delete(id: UUID) {
    transferRepository.findByIdOrNull(id)?.let { tr ->
      SchedulerContext.get().copy(username = SYSTEM_USERNAME).set()
      if (tr.movement != null) {
        tr.makeUnscheduled(MakeUnscheduled)
      } else {
        transferRepository.delete(tr)
      }
    }
  }
}
