package uk.gov.justice.digital.hmpps.hmppstemplatepackagename.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppstemplatepackagename.service.UserMessageDto

// This controller only serves as an endpoint for this kotlin template to call out to.
// This saves the hmpps-template-kotlin from depending on a different service or having to create a new service
// just to act as a dependency.
// It does mean though that there is a dependency on itself here, which is not recommended!
@RestController
// The role here indicates that it is a read only access, normally from other APIs
@PreAuthorize("hasRole('ROLE_TEMPLATE_KOTLIN__RO')")
@RequestMapping(value = ["/example-external-api"], produces = ["application/json"])
class ExampleExternalApiCallResource {
  @GetMapping("/{user}")
  fun getUserMessage(@PathVariable user: String): UserMessageDto = when {
    user.length == 13 -> throw ExampleErrorHandlingNotFoundException()
    user.length % 2 == 0 -> "Found an even user"
    else -> "Found an odd user"
  }.let {
    UserMessageDto(it)
  }
}

// This is mapped in the HmppsTemplateKotlinExceptionHandler to a 404 response
class ExampleErrorHandlingNotFoundException : RuntimeException("User not found")
