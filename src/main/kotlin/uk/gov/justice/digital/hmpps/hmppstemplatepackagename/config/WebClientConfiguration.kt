package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import kotlin.apply as kotlinApply

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.oauth}") val oauthApiBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:90s}") val timeout: Duration,
) {
  @Bean
  fun oauthApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(oauthApiBaseUri, healthTimeout)

  /**
   * TODO
   * Once you have a client registration defined in properties `spring.security.client.registration` then you'll
   *  need to uncomment this @Bean and create both a health and authorized web client.
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
   */
  // @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    return AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    ).kotlinApply { setAuthorizedClientProvider(authorizedClientProvider) }
  }
}

fun WebClient.Builder.authorisedWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, registrationId: String, url: String, timeout: Duration): WebClient {
  val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager).kotlinApply {
    setDefaultClientRegistrationId(registrationId)
  }

  return baseUrl(url)
    .clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(timeout)))
    .filter(oauth2Client)
    .build()
}

fun WebClient.Builder.healthWebClient(url: String, healthTimeout: Duration): WebClient =
  baseUrl(url)
    .clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(healthTimeout)))
    .build()
