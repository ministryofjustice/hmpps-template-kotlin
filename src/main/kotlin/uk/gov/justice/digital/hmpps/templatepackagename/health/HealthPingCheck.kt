package uk.gov.justice.digital.hmpps.templatepackagename.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

// HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
// TODO: Remove the health ping if no call outs to other services are made
@Component("hmppsAuth")
class HmppsAuthHealthPing(@Qualifier("hmppsAuthHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)

// TODO: Example ping health check calling out to other services
@Component("exampleApi")
class ExampleApiHealthPing(@Qualifier("exampleApiHealthWebClient") webClient: WebClient) : HealthPingCheck(webClient)
