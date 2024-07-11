package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@PreAuthorize("hasRole('ROLE_TEMPLATE_KOTLIN__RO')")
class ExampleResource() {

  @GetMapping("/time")
  fun getTime(): LocalDateTime = LocalDateTime.now()
}
