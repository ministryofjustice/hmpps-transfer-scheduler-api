package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import java.util.UUID

data class SyncTransferResponse(
  val dpsId: UUID,
  val eventId: Long?,
  val bookingId: Long?,
  val movementSeq: Long?,
)
