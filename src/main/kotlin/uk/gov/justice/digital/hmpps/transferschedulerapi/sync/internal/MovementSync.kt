package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.getTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyTransit
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.CompleteTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.PersonSummaryService
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.ReferenceId
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovementRequest
import java.util.UUID

@Transactional
@Service
class MovementSync(
  private val rdRepository: ReferenceDataRepository,
  private val movementRepository: MovementRepository,
  private val transferRepository: TransferRepository,
  private val personSummaryService: PersonSummaryService,
) {
  fun sync(personIdentifier: String, request: SyncMovementRequest): ReferenceId = with(request) {
    SchedulerContext.get()
      .copy(requestAt = occurredAt, username = syncUser.username, caseloadId = syncUser.activeCaseloadId).set()
    val rdProvider = rdRepository.rdProvider()
    val existing: Movement? = (
      movement.dpsId?.let { movementRepository.findByIdOrNull(it) }
        ?: movement.legacyId?.let { movementRepository.findByLegacyId(it) }
      )
    val transfer = movement.dpsTransferId?.let {
      if (existing?.transfer?.id == it) existing.transfer else transferRepository.getTransfer(it)
    }
      ?: existing?.transfer?.takeIf { it.stage == TransferStage.UNSCHEDULED }
      ?: movement.unscheduledTransfer(personIdentifier, rdProvider)

    val saved = existing?.updateFrom(movement, transfer, rdProvider)
      ?: transfer.addMovement(movement, rdProvider)
    ReferenceId(saved.id)
  }

  private fun Transfer.addMovement(request: SyncMovement, rdProvider: RdProvider): Movement {
    if (movement != null) throw ConflictException("Movement already exists")
    if (request.active == true) {
      applyTransit(ApplyTransit(request), rdProvider)
    } else {
      withMovement(request, rdProvider).complete(CompleteTransfer, rdProvider)
    }
    return requireNotNull(movement)
  }

  private fun SyncMovement.unscheduledTransfer(personIdentifier: String, rdProvider: RdProvider): Transfer {
    val person = personSummaryService.getWithSave(personIdentifier)
    val statusCode = if (active == true) TransferStatus.Code.IN_TRANSIT else TransferStatus.Code.COMPLETED
    return transferRepository.save(
      Transfer(
        person,
        fromAgyLocId,
        rdProvider.get(reasonCode),
        rdProvider.get(statusCode.name),
        destinationCode,
        rdProvider.get(logisticsCode),
        TransferStage.UNSCHEDULED,
        null,
      ),
    )
  }

  fun delete(id: UUID) {
    movementRepository.findByIdOrNull(id)?.let { mov ->
      SchedulerContext.get().copy(username = SYSTEM_USERNAME).set()
      val transfer = mov.transfer
      if (transfer.stage == TransferStage.UNSCHEDULED || !transfer.isReadyToSchedule()) {
        transferRepository.delete(transfer)
      } else {
        val rdProvider = rdRepository.rdProvider()
        transfer.withMovement(null, rdProvider)
        transfer.complete(CompleteTransfer, rdProvider)
        movementRepository.delete(mov)
      }
    }
  }
}
