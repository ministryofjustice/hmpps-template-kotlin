package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TimeResourceIntTest : IntegrationTestBase() {

  @Test
  fun `should return unauthorized if no token`() {
    webTestClient.get()
      .uri("/time")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should return forbidden if no role`() {
    webTestClient.get()
      .uri("/time")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("status").isEqualTo(403)
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/time")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus().isForbidden
      .expectBody().jsonPath("status").isEqualTo(403)
  }

  @Test
  fun `should return OK`() {
    webTestClient.get()
      .uri("/time")
      .headers(setAuthorisation(roles = listOf("ROLE_TEMPLATE_EXAMPLE")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$").value<String> {
        assertThat(it).startsWith("${LocalDate.now()}")
      }
  }
}
