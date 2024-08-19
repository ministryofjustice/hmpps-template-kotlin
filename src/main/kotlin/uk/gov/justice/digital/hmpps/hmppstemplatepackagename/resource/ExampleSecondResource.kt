package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstemplatepackagename.service.UserMessageDto
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

@RestController
// Protected by a read only role
@PreAuthorize("hasRole('ROLE_TEMPLATE_KOTLIN__RO')")
@RequestMapping(value = ["/example-2"], produces = ["application/json"])
class ExampleSecondResource {

  @GetMapping("/{authSource}")
  fun getSecondExampleUserMessage(@PathVariable authSource: AuthSource): UserMessageDto = when (authSource) {
    AuthSource.NOMIS -> "Found a DPS user"
    AuthSource.DELIUS -> "Found a Delius user"
    AuthSource.AUTH -> "Found an external user"
    else -> "Found a user that authenticated elsewhere"
  }.let {
    UserMessageDto(it)
  }
}
