package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch

import com.fasterxml.jackson.annotation.JsonIgnore

data class PrisonerNumbers(
  val prisonerNumbers: Set<String>,
)

data class Prisoner(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,
  val status: String?,
  val prisonId: String?,
  val lastPrisonId: String?,
  val cellLocation: String?,
) {
  @JsonIgnore
  fun responsiblePrison(): String = (if (status?.startsWith(ACTIVE) == true) lastPrisonId else prisonId) ?: OUT

  companion object {
    private const val ACTIVE = "ACTIVE"
    private const val OUT = "OUT"

    fun fields() = arrayOf(
      Prisoner::prisonerNumber.name,
      Prisoner::firstName.name,
      Prisoner::lastName.name,
      Prisoner::status.name,
      Prisoner::prisonId.name,
      Prisoner::lastPrisonId.name,
      Prisoner::cellLocation.name,
    )

    const val PATTERN: String = "\\w\\d{4}\\w{2}"
  }
}
