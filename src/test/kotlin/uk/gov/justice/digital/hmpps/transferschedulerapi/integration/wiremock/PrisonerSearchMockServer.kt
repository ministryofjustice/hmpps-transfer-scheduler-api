package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import tools.jackson.module.kotlin.jsonMapper
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.cellLocation
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonersearch.PrisonerNumbers
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.WiremockConfig.mockServerConfig

class PrisonerSearchServer : WireMockServer(mockServerConfig(9000)) {
  fun givenPrisoner(prisoner: Prisoner): Prisoner = givenPrisoners(prisoner.prisonId!!, setOf(prisoner.prisonerNumber), listOf(prisoner)).first()

  fun givenPrisoners(
    prisonCode: String,
    prisonNumbers: Set<String>,
    prisoners: List<Prisoner> = prisonNumbers.map { prisoner(prisonCode, it) },
  ): List<Prisoner> {
    stubFor(
      post(urlPathEqualTo("/prisoner-search/prisoner-numbers"))
        .withBearerToken()
        .withRequestBody(equalToJson(jsonMapper().writeValueAsString(PrisonerNumbers(prisonNumbers)), true, true))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper().writeValueAsString(prisoners))
            .withStatus(200),
        ),
    )
    return prisoners
  }

  companion object {
    fun prisoner(
      prisonCode: String,
      personIdentifier: String = personIdentifier(),
      firstName: String = word(8),
      lastName: String = word(12),
      cellLocation: String = cellLocation(),
    ): Prisoner = Prisoner(
      personIdentifier,
      firstName,
      lastName,
      prisonCode,
      prisonCode,
      cellLocation,
    )
  }
}

class PrisonerSearchExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearch = PrisonerSearchServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearch.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearch.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearch.stop()
  }
}
