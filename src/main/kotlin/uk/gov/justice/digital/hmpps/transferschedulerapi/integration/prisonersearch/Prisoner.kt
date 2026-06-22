package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch

data class PrisonerNumbers(
  val prisonerNumbers: Set<String>,
)

data class Prisoner(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,
  val prisonId: String?,
  val lastPrisonId: String?,
  val cellLocation: String?,
) {
  companion object {
    fun fields() = arrayOf(
      Prisoner::prisonerNumber.name,
      Prisoner::firstName.name,
      Prisoner::lastName.name,
      Prisoner::prisonId.name,
      Prisoner::lastPrisonId.name,
      Prisoner::cellLocation.name,
    )

    const val PATTERN: String = "\\w\\d{4}\\w{2}"
  }
}
