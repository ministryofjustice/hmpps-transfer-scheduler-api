package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import java.util.SequencedSet
import java.util.UUID

data class MoveTransfersRequest(
  val from: String,
  val to: String,
  val transferIds: SequencedSet<UUID>,
)
