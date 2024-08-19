package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppstemplatepackagename.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.hmppstemplatepackagename.integration.wiremock.TemplateKotlinApiExtension.Companion.templateKotlinApi
import java.time.LocalDate

class ExampleResourceIntTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /example/time")
  inner class TimeEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/example/time")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/example/time")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/example/time")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      webTestClient.get()
        .uri("/example/time")
        .headers(setAuthorisation(roles = listOf("ROLE_TEMPLATE_KOTLIN__UI")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$").value<String> {
          assertThat(it).startsWith("${LocalDate.now()}")
        }
    }
  }

  @Nested
  @DisplayName("GET /example/user-message")
  inner class UserDetailsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/example/user-message")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/example/user-message")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/example/user-message")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      hmppsAuth.stubGrantToken()
      templateKotlinApi.stubGetSecondExampleUserDetails()
      webTestClient.get()
        .uri("/example/user-message")
        .headers(setAuthorisation(roles = listOf("ROLE_TEMPLATE_KOTLIN__UI")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.message").isEqualTo("A stubbed message")

      templateKotlinApi.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/example-2/NONE")))
    }
  }
}
