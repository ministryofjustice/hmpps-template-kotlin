package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */
@Component
class HealthInfo(buildProperties: BuildProperties) : HealthIndicator {
  private val version: String = buildProperties.version

  override fun health(): Health = Health.up().withDetail("version", version).build()
}

@Component
class MyInfoContributor(@Value("\${application.productId}") private val productId: String) : InfoContributor {

  override fun contribute(builder: Info.Builder) {
    builder.withDetail("productId", productId)
  }
}
