package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ValidStartAndEnd
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged.TransferSearchRequest.Companion.FROM
import java.time.LocalDate

@ValidStartAndEnd
@ValidDateRange(31)
data class TransferPrisonSearchRequest(
  override val start: LocalDate,
  override val end: LocalDate,
  val query: String? = null,
  @Schema(requiredMode = NOT_REQUIRED)
  override val statusCodes: Set<TransferStatus.Code> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  override val reasonCodes: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  val destinationCodes: Set<String> = emptySet(),
  @Schema(requiredMode = NOT_REQUIRED)
  val logisticsCodes: Set<String> = emptySet(),
  val priorityCode: TransferPriority.Code? = null,
  val includeTransferred: Boolean = false,
  override val page: Int = 1,
  override val size: Int = 10,
  override val sort: String = FROM,
) : TransferSearchRequest
