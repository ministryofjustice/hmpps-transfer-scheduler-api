package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfersRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfersResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ScheduleTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.PRE_SCHEDULED_STATUSES

@Transactional
@Service
class BulkScheduleTransfers(
  private val rdRepository: ReferenceDataRepository,
  private val transferRepository: TransferRepository,
  private val personSummaryService: PersonSummaryService,
  private val prisonRegister: PrisonRegisterClient,
) {
  fun merge(request: BulkTransfersRequest): BulkTransfersResponse {
    val rdProvider = rdRepository.rdProvider()
    val people = personSummaryService.retrieveAndSaveAll(request.transfers.map { it.personIdentifier }.toSet())
    val existing = transferRepository.findAllById(request.transfers.map { it.id }).associateBy { it.id }
    val transfers = request.transfers.mapNotNull { tr ->
      existing[tr.id]?.updateFrom(tr, rdProvider)
        ?: people[tr.personIdentifier]?.let { transferRepository.save(tr.asEntity(it, rdProvider)) }
    }
    val prisons = prisonRegister.prisonProvider(transfers.flatMap { listOfNotNull(it.prisonCode, it.destinationCode) }.toSet())
    return BulkTransfersResponse(transfers.map { it.asModel(prisons::get) })
  }

  private fun Transfer.updateFrom(request: BulkTransfer, rdProvider: RdProvider): Transfer = apply {
    applyDestination(ApplyDestination(request.destinationCode))
    applyLogistics(ApplyLogistics(request.logisticsCode), rdProvider)
    applyReason(ApplyReason(request.reasonCode), rdProvider)
    if (request.statusCode == TransferStatus.Code.SCHEDULED && status.code in PRE_SCHEDULED_STATUSES.map { it.name }) {
      with(request.schedule) { applySchedule(ScheduleTransfer(start, comments), rdProvider) }
    } else {
      withSchedule(request.schedule)
    }
  }
}
