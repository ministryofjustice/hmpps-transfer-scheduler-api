package uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes

import jakarta.validation.constraints.NotEmpty
import java.time.LocalDateTime
import java.util.SequencedSet

data class ClashRequest(
  @NotEmpty
  val personIdentifiers: SequencedSet<ClashPersonIdentifier>,
  @NotEmpty
  val ranges: SequencedSet<ClashRange>,
)

data class ClashRange(val start: LocalDateTime, val end: LocalDateTime)
