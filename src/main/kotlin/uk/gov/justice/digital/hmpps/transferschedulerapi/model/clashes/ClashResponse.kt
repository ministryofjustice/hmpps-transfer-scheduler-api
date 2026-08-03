package uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes

import java.time.LocalDateTime

data class ClashResponse(val origin: ClashOrigin, val data: List<PersonClashes>)
data class ClashOrigin(val source: ClashSource)
data class ClashSource(val productId: String, val name: String)

data class PersonClashes(val personIdentifier: ClashPersonIdentifier, val clashes: List<Clash>)

data class Clash(
  val start: LocalDateTime,
  val end: LocalDateTime,
  val description: Description,
  val location: Location,
  val additionalInformation: AdditionalInformation,
) {
  data class Description(val full: String, val short: String)
  data class Location(val description: String)
  data class AdditionalInformation(val prisonCode: String)
}
