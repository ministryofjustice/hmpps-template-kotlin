package uk.gov.justice.digital.hmpps.hmppstemplatepackagename

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * TODO
 * This is just an example of what a secured endpoint might look like.
 * Remove this class and associated tests in [TimeResourceIntTest] and replace with your own implementation.
 *
 * Note that the `@ConditionalOnProperty` annotation is used to ensure that this endpoint is only enabled when the template
 * is deployed to the dev environment and in tests. Just in case you forget to remove this class after bootstrapping. This
 * isn't a pattern you should use in your own code.
 */
@ConditionalOnExpression("'\${api.base.url.hmpps-auth}' == 'https://sign-in-dev.hmpps.service.justice.gov.uk/auth' OR '\${api.base.url.hmpps-auth}' == 'http://localhost:8090/auth'")
@RestController
@RequestMapping("/time")
class TimeResource {

  @PreAuthorize("hasRole('TEMPLATE_EXAMPLE')")
  @GetMapping
  fun getTime() = LocalDateTime.now().toString()
}
