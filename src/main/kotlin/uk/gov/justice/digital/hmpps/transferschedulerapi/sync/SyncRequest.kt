package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

interface SyncRequest

interface NumericLegacyIdRequest : SyncRequest {
  val legacyId: Long
}

interface StringLegacyIdRequest : SyncRequest {
  val legacyId: String
}
