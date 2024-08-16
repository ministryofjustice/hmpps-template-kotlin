package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder

@Service
class TemplateKotlinApiService(
  private val templateKotlinApiWebClient: WebClient,
  private val hmppsAuthenticationHolder: HmppsAuthenticationHolder,
) {
  fun getUserMessage(): UserMessageDto =
    templateKotlinApiWebClient.get()
      // Note that we don't use string interpolation ("/$authSource").
      // This is important - using string interpolation causes each uri to be added as a separate path in app
      // insights and you'll run out of memory in your app
      .uri("/example-2/{authSource}", hmppsAuthenticationHolder.authSource)
      .retrieve()
      .bodyToMono(UserMessageDto::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      // our endpoint always returns a response body, so okay to enforce that
      .block()!!
}

data class UserMessageDto(
  val message: String,
)
