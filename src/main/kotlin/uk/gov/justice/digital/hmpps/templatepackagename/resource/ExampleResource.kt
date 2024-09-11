package uk.gov.justice.digital.hmpps.templatepackagename.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.templatepackagename.service.ExampleApiService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

// This controller is expected to be called from the UI - so the hmpps-template-typescript project.
// TODO: This is an example and should renamed / replaced
@RestController
// Role here is specific to the UI.
@PreAuthorize("hasRole('ROLE_TEMPLATE_KOTLIN__UI')")
@RequestMapping(value = ["/example"], produces = ["application/json"])
class ExampleResource(private val exampleApiService: ExampleApiService) {

  @GetMapping("/time")
  @Tag(name = "Examples")
  @Operation(
    summary = "Retrieve today's date and time",
    description = "This is an example endpoint that calls a service to return the current date and time. Requires role ROLE_TEMPLATE_KOTLIN__UI",
    security = [SecurityRequirement(name = "template-kotlin-ui-role")],
    responses = [
      ApiResponse(responseCode = "200", description = "today's date and time"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getTime(): LocalDateTime = exampleApiService.getTime()

  @GetMapping("/message/{parameter}")
  @Tag(name = "Popular")
  @Operation(
    summary = "Example message endpoint to call another API",
    description = """This is an example endpoint that calls back to the kotlin template.
      It will return a 404 response as the /example-external-api endpoint hasn't been implemented, so we use wiremock
      in integration tests to simulate other responses.
      Requires role ROLE_TEMPLATE_KOTLIN__UI""",
    security = [SecurityRequirement(name = "template-kotlin-ui-role")],
    responses = [
      ApiResponse(responseCode = "200", description = "a message with a parameter"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getMessage(@PathVariable parameter: String) = exampleApiService.exampleGetExternalApiCall(parameter)
}
