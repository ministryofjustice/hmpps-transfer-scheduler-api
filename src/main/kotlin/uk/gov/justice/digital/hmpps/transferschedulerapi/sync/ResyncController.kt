package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.OpenApiTags
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.TransfersResync
import java.time.LocalDateTime
import java.util.UUID

@Tag(name = OpenApiTags.SYNC)
@RestController
@RequestMapping("resync/transfers")
@PreAuthorize("hasRole('${Roles.NOMIS_SYNC}')")
class ResyncController(private val resync: TransfersResync) {
  @PutMapping("/{personIdentifier}")
  fun resync(
    @PathVariable personIdentifier: String,
    @Valid @RequestBody request: ResyncTransfersRequest,
  ): ResyncResponse = resync.all(personIdentifier, request)
}

data class ResyncTransfersRequest(val transfers: List<ResyncTransfer>, val unscheduledMovements: List<ResyncMovement>) {
  @JsonIgnore
  fun isEmpty(): Boolean = transfers.isEmpty() && unscheduledMovements.isEmpty()

  fun transferIds(): Pair<Set<Long>, Set<UUID>> {
    val (legacyIds, ids) = transfers.map { re -> requireNotNull(re.transfer.eventId) to re.transfer.dpsId }.unzip()
    return legacyIds.toSet() to ids.filterNotNull().toSet()
  }

  fun movementIds(): Pair<Set<String>, Set<UUID>> {
    val (legacyIds, ids) = (
      unscheduledMovements.map { requireNotNull(it.movement.legacyId) to it.movement.dpsId } +
        transfers.mapNotNull { it.movement }.map { requireNotNull(it.movement.legacyId) to it.movement.dpsId }
      ).unzip()
    return legacyIds.toSet() to ids.filterNotNull().toSet()
  }
}

data class ResyncTransfer(
  val transfer: SyncTransfer,
  val created: AtAndBy,
  val modified: AtAndBy?,
  val movement: ResyncMovement?,
)

data class ResyncMovement(
  val movement: SyncMovement,
  val created: AtAndBy,
  val modified: AtAndBy?,
)

data class AtAndBy(val at: LocalDateTime, val by: String)

data class ResyncResponse(val transfers: List<TransferMapping>, val unscheduledMovements: List<TransferMovementMapping>)
data class TransferMapping(val dpsId: UUID, val eventId: Long, val movement: TransferMovementMapping?)
data class TransferMovementMapping(
  val dpsId: UUID,
  @JsonProperty("offenderBookId") val bookingId: Long,
  @JsonProperty("movementSeq") val sequenceNumber: Int,
)
