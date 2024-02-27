@file:Suppress("ktlint:standard:filename")

package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component("hmppsAuth")
class HmppsAuthHealthPingCheck(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

// TODO add this back in if you create a bean called `prisonApiHealthWebClient`
// @Component("prisonApi")
// class PrisonApiHealthPingCheck(@Qualifier("prisonApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
