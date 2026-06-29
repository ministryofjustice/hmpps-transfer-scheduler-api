package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata

enum class TransferReasonCode {
  TR_28,
  ACCVISIT,
  AS,
  APPEALS,
  COMP,
  PROD,
  MED,
  NOTR,
  OTHER,
  OJ,
  OVCROW,
  PRES,
  PROAT,
  SEC,
  ;

  companion object {
    fun randomCode(): String = entries.random().name.replace("TR_", "")
  }
}
