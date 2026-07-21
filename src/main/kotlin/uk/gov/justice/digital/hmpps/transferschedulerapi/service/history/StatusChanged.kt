package uk.gov.justice.digital.hmpps.transferschedulerapi.service.history

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import java.time.LocalDateTime

data class StatusChanged(
  val username: String,
  val occurredAt: LocalDateTime,
  val from: TransferStatus.Code?,
  val to: TransferStatus.Code,
)
