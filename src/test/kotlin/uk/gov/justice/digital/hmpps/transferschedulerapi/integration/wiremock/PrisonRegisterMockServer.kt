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
import org.springframework.http.HttpStatus
import tools.jackson.module.kotlin.jsonMapper
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister.PrisonsByIdsRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.WiremockConfig.mockServerConfig
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Prison

class PrisonRegisterMockServer : WireMockServer(mockServerConfig(9005)) {
  fun givenPrison(prison: Prison = prison()): Prison = givenPrisons(setOf(prison)).first()

  fun givenPrisons(
    prisons: Set<Prison>,
    prisonCodes: Set<String> = setOf(),
  ): Set<Prison> {
    val request = PrisonsByIdsRequest((prisonCodes.takeIf { it.isNotEmpty() } ?: prisons.map { it.code }.toSet()))
    stubFor(
      post(urlPathEqualTo("/prisons/prisonsByIds"))
        .withBearerToken()
        .withRequestBody(
          equalToJson(
            jsonMapper().writeValueAsString(request),
            true,
            true,
          ),
        ).willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(
              jsonMapper().writeValueAsString(
                prisons.map {
                  mapOf(
                    "prisonId" to it.code,
                    "prisonName" to it.name,
                  )
                },
              ),
            ),
        ),
    )
    return prisons
  }

  companion object {
    fun prison(code: String = prisonCode(), name: String = word(10)) = Prison(code, name)
  }
}

class PrisonerRegisterExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonRegister = PrisonRegisterMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonRegister.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonRegister.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonRegister.stop()
  }
}
