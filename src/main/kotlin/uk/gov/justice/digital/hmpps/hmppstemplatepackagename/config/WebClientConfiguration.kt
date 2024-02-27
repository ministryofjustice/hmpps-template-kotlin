package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.hmpps-auth}") val hmppsAuthBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:90s}") val timeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient =
    builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  /**
   * TODO
   * Once you have a client registration defined in properties `spring.security.client.registration` then you'll
   *  need to create both a health and authorized web client.
   *
   * e.g. if your client registration config looks like this (registrationId is `prison-api`):
   * ```
   * spring:
   *   security:
   *     client:
   *       registration:
   *            prison-api:
   *             provider: hmpps-auth
   *             client-id: ${prison-api.client.id}
   *             client-secret: ${prison-api.client.secret}
   *             authorization-grant-type: client_credentials
   *             scope: read
   * ```
   * Then you need to create web clients in this class as follows:
   * ```
   *   @Bean
   *   fun prisonApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonApiBaseUri, healthTimeout)
   *
   *   @Bean
   *   fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
   *     builder.authorisedWebClient(authorizedClientManager, registrationId = "prison-api", url = prisonApiBaseUri, timeout)
   * ```
   * Though if you are using a reactive web server the corresponding builder functions should be `reactiveHealthWebClient` and `reactiveAuthorisedWebClient`.
   */
}
