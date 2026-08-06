package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.StageRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ValidDateRange
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ValidStartAndEnd
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest.Companion.PLAN_REQUESTED
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest.Companion.REASON
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest.Companion.SCHEDULE_START
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest.Companion.STATUS
import java.time.LocalDate

interface QueryRequest {
  val query: String?
}

const val PLANNING = "PLANNING"
const val SCHEDULED = "SCHEDULED"

@Schema(
  discriminatorProperty = "stage",
  discriminatorMapping = [
    DiscriminatorMapping(value = PLANNING, schema = PlanningSearchRequest::class),
    DiscriminatorMapping(value = SCHEDULED, schema = ScheduledSearchRequest::class),
  ],
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "stage", visible = true)
@JsonSubTypes(
  value = [
    JsonSubTypes.Type(value = PlanningSearchRequest::class, name = PLANNING),
    JsonSubTypes.Type(value = ScheduledSearchRequest::class, name = SCHEDULED),
  ],
)
sealed interface PrisonTransferSearchRequest :
  TransferSearchRequest,
  StageRequest {
  val destinationCodes: Set<String>
  val logisticsCodes: Set<String>

  @get:Schema(type = "string")
  override val stage: TransferStage
}

@ValidStartAndEnd
@ValidDateRange(31)
data class PlanningSearchRequest(
  val startAndEndType: StartAndEndType = StartAndEndType.START,
  override val start: LocalDate? = null,
  override val end: LocalDate? = null,
  override val query: String? = null,
  @Schema(requiredMode = NOT_REQUIRED)
  override val statusCodes: Set<TransferStatus.Code> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val reasonCodes: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val destinationCodes: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val logisticsCodes: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  val priorityCodes: Set<TransferPriority.Code> = emptySet(),
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = PLAN_REQUESTED,
) : PrisonTransferSearchRequest,
  QueryRequest {
  override val stage: TransferStage = TransferStage.PLANNING
  override fun validSortFields(): Set<String> = setOf(PLAN_REQUESTED, SCHEDULE_START, REASON, STATUS)
  enum class StartAndEndType {
    REQUESTED_ON,
    START,
  }

  @JsonIgnore
  fun isRequestedOnSearch(): Boolean = startAndEndType == StartAndEndType.REQUESTED_ON && start != null && end != null
}

@ValidStartAndEnd
@ValidDateRange(31)
data class ScheduledSearchRequest(
  override val start: LocalDate,
  override val end: LocalDate,
  override val query: String? = null,
  @Schema(requiredMode = NOT_REQUIRED)
  override val statusCodes: Set<TransferStatus.Code> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val reasonCodes: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val destinationCodes: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val logisticsCodes: Set<String> = emptySet(),
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = SCHEDULE_START,
) : PrisonTransferSearchRequest,
  QueryRequest {
  override val stage: TransferStage = TransferStage.SCHEDULED
  override fun validSortFields(): Set<String> = setOf(SCHEDULE_START, REASON, STATUS)
}
