package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class TemplateKotlinApiMockServer : WireMockServer(8091) {
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
}

class TemplateKotlinApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val templateKotlinApi = TemplateKotlinApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = templateKotlinApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = templateKotlinApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = templateKotlinApi.stop()
}
