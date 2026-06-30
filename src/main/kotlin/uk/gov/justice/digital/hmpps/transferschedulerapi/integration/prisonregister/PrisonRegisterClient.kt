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

  fun prisonProvider(ids: Set<String>): PrisonProvider = PrisonProvider(findPrisons(ids).block()!!)
}

data class PrisonsByIdsRequest(val prisonIds: Set<String>)

class PrisonProvider(prisons: List<Prison>) {
  private val prisons = prisons.associateBy(Prison::code)
  fun get(code: String): Prison = prisons[code] ?: Prison.default(code)
  fun containsAll(codes: Set<String>) = prisons.keys.containsAll(codes)
}
