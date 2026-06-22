package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.prisonregister

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.retryOnTransientException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Prison

@Service
class PrisonRegisterClient(
  @Qualifier("prisonRegisterApiWebClient") private val webClient: WebClient,
) {
  fun findPrisons(ids: Set<String>): Mono<List<Prison>> = if (ids.isEmpty()) {
    Mono.just(emptyList())
  } else {
    webClient
      .post()
      .uri("/prisons/prisonsByIds")
      .bodyValue(PrisonsByIdsRequest(ids))
      .retrieve()
      .bodyToMono<List<Prison>>()
      .retryOnTransientException()
  }

  fun findPrison(code: String): Mono<Prison> = findPrisons(setOf(code)).map { prs -> prs.firstOrNull { it.code == code } ?: Prison.default(code) }
}

data class PrisonsByIdsRequest(val prisonIds: Set<String>)
