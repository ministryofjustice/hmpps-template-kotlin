package uk.gov.justice.digital.hmpps.templatepackagename.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

// TODO: Remove / replace this mock server as it currently calls the Example API (itself)
class ExampleApiMockServer : WireMockServer(8091) {
  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("""{"status":"${if (status == 200) "UP" else "DOWN"}"}""")
          .withStatus(status),
      ),
    )
  }

  fun stubExampleExternalApiUserMessage() {
    stubFor(
      get(urlPathMatching("/example-external-api/[a-zA-Z]*"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""{ "message": "A stubbed message" }"""),
        ),
    )
  }

  fun stubExampleExternalApiNotFound() {
    stubFor(
      get(urlPathMatching("/example-external-api/[a-zA-Z]*"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody("""{ "userMessage": "A stubbed message" }"""),
        ),
    )
  }
}

class ExampleApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val exampleApi = ExampleApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = exampleApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = exampleApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = exampleApi.stop()
}
