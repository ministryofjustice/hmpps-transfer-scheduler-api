package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import java.time.LocalDateTime
import java.util.UUID

interface IdRequest {
  val id: UUID
}

data class BulkTransfersRequest(val transfers: List<BulkTransfer>)

data class BulkTransfer(
  val personIdentifier: String,
  val statusCode: TransferStatus.Code,
  override val destinationCode: String,
  override val logisticsCode: String,
  override val reasonCode: String,
  override val start: LocalDateTime,
  override val comments: String?,
  override val id: UUID,
) : TransferRequest,
  ScheduleRequest,
  IdRequest {
  @JsonIgnore
  override val plan: PlanRequest? = null

  @JsonIgnore
  override val schedule: ScheduleRequest = this
  override fun initialStatusCode(): TransferStatus.Code = statusCode
  override fun initialStage(): TransferStage = TransferStage.SCHEDULED
}

data class BulkTransfersResponse(val transfers: List<Transfer>)
