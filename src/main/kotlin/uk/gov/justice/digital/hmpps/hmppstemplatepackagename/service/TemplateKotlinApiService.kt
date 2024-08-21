package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class TemplateKotlinApiService(
  private val templateKotlinApiWebClient: WebClient,
) {
  fun getUserMessage(user: String): UserMessageDto? =
    templateKotlinApiWebClient.get()
      // Note that we don't use string interpolation ("/${user}").
      // This is important - using string interpolation causes each uri to be added as a separate path in app
      // insights and you'll run out of memory in your app.
      .uri("/example-external-api/{user}", user)
      .retrieve()
      .bodyToMono(UserMessageDto::class.java)
      // if the endpoint returns a not found response (404) then treat as empty rather than throwing a server error
      // other options would be to re-throw the not found and use the controller advice to return a 404
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
}

data class UserMessageDto(
  val message: String,
)
