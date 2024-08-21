package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExampleExternalApiCallResourceIntTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /example-external-api/{user}")
  inner class ExampleExternalApiEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/example-external-api/NONE")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/example-external-api/NONE")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/example-external-api/NONE")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      webTestClient.get()
        .uri("/example-external-api/odd")
        .headers(setAuthorisation(roles = listOf("ROLE_TEMPLATE_KOTLIN__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.message").isEqualTo("Found an odd user")
    }

    @Test
    fun `should return NOT_FOUND if user not found`() {
      webTestClient.get()
        // code will throw exception if the user is 13 characters long
        .uri("/example-external-api/thirteenchars")
        .headers(setAuthorisation(roles = listOf("ROLE_TEMPLATE_KOTLIN__RO")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.userMessage").isEqualTo("Not found error: User not found")
    }
  }
}
