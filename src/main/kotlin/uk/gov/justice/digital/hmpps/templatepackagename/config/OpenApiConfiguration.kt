package uk.gov.justice.digital.hmpps.templatepackagename.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://template-kotlin-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://template-kotlin-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://template-kotlin.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .tags(
      listOf(
        // TODO: Remove the Popular and Examples tag and start adding your own tags to group your resources
        Tag().name("Popular")
          .description("The most popular endpoints. Look here first when deciding which endpoint to use."),
        Tag().name("Examples").description("Endpoints for searching for a prisoner within a prison"),
      ),
    )
    .info(
      Info().title("HMPPS Template Kotlin").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    // TODO: Remove the default security schema and start adding your own schemas and roles to describe your
    // service authorisation requirements
    .components(
      Components().addSecuritySchemes(
        "template-kotlin-ui-role",
        SecurityScheme().addBearerJwtRequirement("ROLE_TEMPLATE_KOTLIN__UI"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("template-kotlin-ui-role", listOf("read")))
}

private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme =
  type(SecurityScheme.Type.HTTP)
    .scheme("bearer")
    .bearerFormat("JWT")
    .`in`(SecurityScheme.In.HEADER)
    .name("Authorization")
    .description("A HMPPS Auth access token with the `$role` role.")
