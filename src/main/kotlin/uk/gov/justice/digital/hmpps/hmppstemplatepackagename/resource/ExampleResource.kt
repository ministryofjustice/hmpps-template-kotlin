package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstemplatepackagename.service.TemplateKotlinApiService
import java.time.LocalDateTime

@RestController
// Protected by a role specific to the ui - called only from the hmpps-template-typescript project
@PreAuthorize("hasRole('ROLE_TEMPLATE_KOTLIN__UI')")
@RequestMapping(value = ["/example"], produces = ["application/json"])
class ExampleResource(private val templateKotlinApiService: TemplateKotlinApiService) {

  @GetMapping("/time")
  fun getTime(): LocalDateTime = LocalDateTime.now()

  @GetMapping("/user-message")
  fun getUserMessage() = templateKotlinApiService.getUserMessage()
}
