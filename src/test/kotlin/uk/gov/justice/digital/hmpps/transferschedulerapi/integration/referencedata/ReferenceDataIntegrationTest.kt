package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.referencedata.CodedDescription
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.referencedata.ReferenceDataResponse

class ReferenceDataIntegrationTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(REFERENCE_DATA_URL, "any-domain")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getReferenceData("any-domain", "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 if invalid domain`() {
    getReferenceData("any-domain").expectStatus().isNotFound
  }

  @Test
  fun `sorts by sequence number`() {
    val rd =
      getReferenceData("transfer-status")
        .expectStatus()
        .isOk
        .expectBody<ReferenceDataResponse>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).containsExactly(
      CodedDescription("PLANNING", "Awaiting details"),
      CodedDescription("READY_TO_SCHEDULE", "Ready to schedule"),
      CodedDescription("SCHEDULED", "Scheduled"),
      CodedDescription("CANCELLED", "Cancelled"),
      CodedDescription("EXPIRED", "Expired"),
      CodedDescription("IN_TRANSIT", "In transit"),
      CodedDescription("COMPLETED", "Completed"),
    )
  }

  @ParameterizedTest
  @MethodSource("referenceDataDomains")
  fun `200 ok - can retrieve reference data domains with correct role`(domain: String) {
    val rd =
      getReferenceData(domain)
        .expectStatus()
        .isOk
        .expectBody<ReferenceDataResponse>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).isNotEmpty
  }

  private fun getReferenceData(
    domain: String,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .get()
    .uri(REFERENCE_DATA_URL, domain)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val REFERENCE_DATA_URL = "/reference-data/{domain}"

    @JvmStatic
    fun referenceDataDomains() = ReferenceDataDomain.Code.entries.map { rdd ->
      Arguments.of(rdd.name)
      Arguments.of(rdd.name.lowercase())
      Arguments.of(rdd.name.lowercase().replace("_", "-"))
    }
  }
}
