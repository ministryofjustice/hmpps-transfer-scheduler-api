package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata

enum class TransferLogisticsCode {
  A,
  L,
  N,
  P,
  U,
  Z,
  GEOAME,
  GROUP4,
  HMPS,
  PREM,
  PECS,
  REL,
  ;

  companion object {
    fun randomCode(): String = entries.random().name
  }
}
