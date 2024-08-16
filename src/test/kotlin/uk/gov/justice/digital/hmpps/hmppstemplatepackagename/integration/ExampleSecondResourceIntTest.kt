package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExampleSecondResourceIntTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /example-2/{authSource}}")
  inner class SecondExampleEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/example-2/NONE")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/example-2/NONE")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/example-2/NONE")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      webTestClient.get()
        .uri("/example-2/DELIUS")
        .headers(setAuthorisation(roles = listOf("ROLE_TEMPLATE_KOTLIN__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.message").isEqualTo("Found a Delius user")
    }
  }
}
