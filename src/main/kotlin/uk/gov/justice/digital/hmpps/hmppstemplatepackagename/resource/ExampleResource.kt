package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstemplatepackagename.service.TemplateKotlinApiService
import java.time.LocalDateTime

// This controller is expected to be called from the UI - so the hmpps-template-typescript project.
@RestController
// Role here is specific to the UI.
@PreAuthorize("hasRole('ROLE_TEMPLATE_KOTLIN__UI')")
@RequestMapping(value = ["/example"], produces = ["application/json"])
class ExampleResource(private val templateKotlinApiService: TemplateKotlinApiService) {

  @GetMapping("/time")
  fun getTime(): LocalDateTime = LocalDateTime.now()

  @GetMapping("/user-message/{user}")
  fun getUserMessage(@PathVariable user: String) = templateKotlinApiService.getUserMessage(user)
}
