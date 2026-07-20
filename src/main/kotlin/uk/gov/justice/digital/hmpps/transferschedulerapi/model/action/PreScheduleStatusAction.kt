package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus

interface PreScheduleStatusAction {
  fun updatePreScheduleStatus(transfer: Transfer, rdProvider: RdProvider) {
    if (transfer.isReadyToSchedule() && transfer.status.code == TransferStatus.Code.PLANNING.name) {
      transfer.applyStatus(TransferStatus.Code.READY_TO_SCHEDULE, rdProvider)
    } else if (!transfer.isReadyToSchedule() && transfer.status.code == TransferStatus.Code.READY_TO_SCHEDULE.name) {
      transfer.applyStatus(TransferStatus.Code.PLANNING, rdProvider)
    }
  }
}
