package uk.gov.justice.digital.hmpps.hmppstemplatepackagename

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/time")
class TimeResource {

  @PreAuthorize("hasRole('TEMPLATE_EXAMPLE')")
  @GetMapping
  fun getTime() = "${LocalDateTime.now()}"
}
