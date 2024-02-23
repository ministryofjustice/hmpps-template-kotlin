package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

abstract class HealthCheck(private val webClient: WebClient) : ReactiveHealthIndicator {

  override fun health(): Mono<Health> = webClient.get()
    .uri("/health/ping")
    .retrieve()
    .toEntity(String::class.java)
    .flatMap { Mono.just(Health.up().withDetail("HttpStatus", it?.statusCode).build()) }
    .onErrorResume(WebClientResponseException::class.java) {
      Mono.just(
        Health.down(it).withDetail("body", it.responseBodyAsString).withDetail("HttpStatus", it.statusCode).build(),
      )
    }
    .onErrorResume(Exception::class.java) { Mono.just(Health.down(it).build()) }
}

@Component("hmppsAuthApi")
class OAuthApiHealth(@Qualifier("oauthApiHealthWebClient") webClient: WebClient) : HealthCheck(webClient)
