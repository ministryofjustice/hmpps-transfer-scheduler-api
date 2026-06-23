package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import com.fasterxml.jackson.annotation.JsonAlias

data class Prison(@JsonAlias("prisonId") val code: String, @JsonAlias("prisonName") val name: String) {
  companion object {
    fun default(code: String): Prison = Prison(code = code, name = code)
  }
}
