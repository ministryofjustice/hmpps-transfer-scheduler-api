package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import java.time.LocalDateTime

interface SyncRequest

interface NumericLegacyIdRequest : SyncRequest {
  val legacyId: Long?
}

interface StringLegacyIdRequest : SyncRequest {
  val legacyId: String?
}

data class SyncUser(val username: String, val activeCaseloadId: String?)

data class SyncTransferRequest(val occurredAt: LocalDateTime, val syncUser: SyncUser, val transfer: SyncTransfer)

data class SyncMovementRequest(val occurredAt: LocalDateTime, val syncUser: SyncUser, val movement: SyncMovement)
