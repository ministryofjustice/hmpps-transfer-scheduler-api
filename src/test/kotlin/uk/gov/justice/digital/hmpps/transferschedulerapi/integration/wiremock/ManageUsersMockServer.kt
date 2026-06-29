package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import tools.jackson.module.kotlin.jsonMapper
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.manageusers.UserDetails
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.WiremockConfig.mockServerConfig

class ManageUsersServer : WireMockServer(mockServerConfig(8095)) {

  fun givenUser(userDetails: UserDetails = user(), status: HttpStatus = HttpStatus.OK): UserDetails {
    val response = aResponse().withHeader("Content-Type", "application/json")
    if (status == HttpStatus.OK) {
      response.withBody(jsonMapper().writeValueAsString(userDetails))
    }
    stubFor(
      get("/users/${userDetails.username}")
        .withBearerToken()
        .willReturn(response.withStatus(status.value())),
    )
    return userDetails
  }

  companion object {
    fun user(username: String = username(), name: String = word(8) + word(8)) = UserDetails(username, name)
  }
}

class ManageUsersExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsers = ManageUsersServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsers.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsers.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsers.stop()
  }
}
