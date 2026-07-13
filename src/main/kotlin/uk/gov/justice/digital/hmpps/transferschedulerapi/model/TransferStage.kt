package uk.gov.justice.digital.hmpps.transferschedulerapi.model

enum class TransferStage {
  PLANNING,
  SCHEDULED,
}

interface StageRequest {
  val stage: TransferStage?
}
