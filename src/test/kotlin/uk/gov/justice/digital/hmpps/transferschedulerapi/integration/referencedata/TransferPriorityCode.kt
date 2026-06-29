package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata

enum class TransferPriorityCode {
  TP1,
  TP2,
  TP3,
  ;

  companion object {
    fun randomCode(): String = entries.random().name.replace("TP", "")
  }
}
