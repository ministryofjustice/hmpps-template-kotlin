package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDateTime

// This is an example of how to write a service calling out to another service.  In this case we have wired up the
// template kotlin with itself so that the template doesn't depend on any other services.
@Service
class ExampleApiService(
  private val templateKotlinApiWebClient: WebClient,
) {
  fun getTime(): LocalDateTime = LocalDateTime.now()

  fun exampleGetExternalApiCall(parameter: String): TemplateMessageDto? =
    templateKotlinApiWebClient.get()
      // Note that we don't use string interpolation ("/${parameter}").
      // This is important - using string interpolation causes each uri to be added as a separate path in app
      // insights and you'll run out of memory in your app.
      .uri("/example-external-api/{parameter}", parameter)
      .retrieve()
      .bodyToMono(TemplateMessageDto::class.java)
      // if the endpoint returns a not found response (404) then treat as empty rather than throwing a server error
      // other options would be to re-throw the not found and use the controller advice to return a 404
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
}

data class TemplateMessageDto(
  val message: String,
)
