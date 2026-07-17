package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import jakarta.validation.Valid
import java.time.LocalDateTime

interface SyncRequest

interface NumericLegacyIdRequest : SyncRequest {
  val legacyId: Long?
}

interface StringLegacyIdRequest : SyncRequest {
  val legacyId: String?
}

data class SyncUser(val username: String, val activeCaseloadId: String?)

data class SyncTransferRequest(
  val occurredAt: LocalDateTime,
  val syncUser: SyncUser,
  @Valid val transfer: SyncTransfer,
)
