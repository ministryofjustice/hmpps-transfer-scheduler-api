package uk.gov.justice.digital.hmpps.transferschedulerapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration
import java.time.Duration.ofSeconds

@Configuration
class WebClientConfiguration(
  @Value($$"${integration.manage-users.url}") private val manageUsersBaseUri: String,
  @Value($$"${integration.nomis-migration.url}") private val nomisMigrationBaseUri: String,
  @Value($$"${integration.prison-register.url}") private val prisonRegisterBaseUri: String,
  @Value($$"${integration.prisoner-search.url}") private val prisonerSearchBaseUri: String,
) {
  @Bean
  fun manageUsersWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) = authorisedWebClient(manageUsersBaseUri, builder, authorizedClientManager)

  @Bean
  fun nomisMigrationWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) = authorisedWebClient(nomisMigrationBaseUri, builder, authorizedClientManager, ofSeconds(10))

  @Bean
  fun prisonRegisterApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder): WebClient = authorisedWebClient(prisonRegisterBaseUri, builder, authorizedClientManager)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) = authorisedWebClient(prisonerSearchBaseUri, builder, authorizedClientManager)

  fun authorisedWebClient(
    url: String,
    builder: Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    timeout: Duration = Companion.timeout,
    registrationId: String = DEFAULT_REGISTRATION_ID,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId, url, timeout)

  companion object {
    const val DEFAULT_REGISTRATION_ID = "default"
    private val timeout: Duration = ofSeconds(2)
  }
}
