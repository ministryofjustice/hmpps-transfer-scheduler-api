package uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata

class RdProvider(rd: List<ReferenceData>) {
  val rdMap = rd.associateBy { it::class to it.code }
  inline fun <reified T : ReferenceData> find(code: String): T? = rdMap[T::class to code] as? T

  inline fun <reified T : ReferenceData> get(code: String): T = find(code) ?: throw IllegalArgumentException("Reference data not recognised")
}
