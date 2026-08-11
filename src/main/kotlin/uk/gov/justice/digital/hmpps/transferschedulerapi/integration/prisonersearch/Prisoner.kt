package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary

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
  }
}

data class Prisoners(val content: List<Prisoner>) {
  private val map = content.associateBy { it.prisonerNumber }

  fun personIdentifiers(): Set<String> = map.keys

  @JsonIgnore
  val size = map.keys.size

  @JsonIgnore
  fun isEmpty() = map.keys.isEmpty()

  operator fun get(prisonerNumber: String): Prisoner? = map[prisonerNumber]
}

fun PersonSummary.asPrisoner() = Prisoner(identifier, firstName, lastName, prisonCode, prisonCode, cellLocation)
