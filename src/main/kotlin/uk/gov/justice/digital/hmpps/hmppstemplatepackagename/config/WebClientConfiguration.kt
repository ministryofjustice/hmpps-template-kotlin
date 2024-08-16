package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${template-kotlin-api.url}") val templateKotlinApiBaseUri: String,
  @Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:20s}") val timeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  // This is an example health bean for checking other services and should be removed / replaced
  @Bean
  fun templateKotlinApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(templateKotlinApiBaseUri, healthTimeout)

  // This is an example bean for calling other services and should be removed / replaced
  @Bean
  fun templateKotlinApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    builder.authorisedWebClient(authorizedClientManager, registrationId = "template-kotlin-api", url = templateKotlinApiBaseUri, timeout)
}
