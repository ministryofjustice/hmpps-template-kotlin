# Money to prisoners API

## Project Overview

A Kotlin Spring Boot microservice and internal admin interface for the Prisoner Money suite of apps. Handles prisoner money transfers, credits, disbursements, payments, and related security/account management.

## Tech Stack

- Kotlin, Spring Boot (via `uk.gov.justice.hmpps.gradle-spring-boot` plugin)
- Java 25 (Eclipse Temurin)
- Gradle (Kotlin DSL)
- PostgreSQL 14+
- Docker & Docker Compose for local services
- Kubernetes + Helm for deployment
- HMPPS Auth for authentication

## Development Rules & Workflow

### Test-Driven Development (TDD) - NON-NEGOTIABLE
*   **Write Tests First**: Every single line of production code must be written in response to a failing test.
*   **Red-Green-Refactor**:
  1.  **Red**: Write a minimal failing test. Confirm it fails.
  2.  **Green**: Implement the simplest code to make the test pass.
  3.  **Refactor**: Clean up the code while keeping tests green.
*   **No Code Without Tests**: Do not implement features, bug fixes, or refactors without corresponding test cases.
*   **Keep Tests Passing**: After every code edit, ensure the test suite passes.

### Testing Standards
*   **Framework**: Use JUnit 5 and Mockito for all testing.
*   **Coverage**: Aim for 100% coverage on new logic.
*   **Isolation**: Tests must be unit-tested in isolation where possible.

### Workflow Commands
*   Run tests: `./gradlew test`
*   Run linter: `./gradlew ktlintFormat`

### Guidelines
*   Think deeply before making any edits.
*   If you find yourself writing production code without a failing test, STOP immediately and write the test first.

## Common Commands

### Build

```bash
./gradlew clean assemble                         # compile and build JAR
./gradlew check                                  # run tests + linting
BUILD_NUMBER=1_0_0 ./gradlew assemble check      # full verify with version
```

### Running Locally

```bash
# Full stack via Docker Compose (app + auth + database)
docker compose pull && docker compose up

# Run only dependencies (for IntelliJ / local dev)
docker compose pull && docker compose up --scale hmpps-money-to-prisoners-api=0
# Then run the app in your IDE with Spring profile: dev
```

### Running Tests

```bash
./gradlew test                                    # all tests
./gradlew test --tests "uk.gov.justice.digital.hmpps.credit.*"   # one package
./gradlew test --tests "uk.gov.justice.digital.hmpps.credit.CreditControllerTest"  # one class
```

### Linting

```bash
./gradlew ktlintCheck                             # check Kotlin code style
./gradlew ktlintFormat                            # auto-fix style issues
```

## Code Style

- **Linter/formatter:** ktlint (bundled via the HMPPS Gradle plugin)
- **Kotlin conventions:** follow standard Kotlin coding conventions
- **Indentation:** 4 spaces for Kotlin/Java
- **Trailing whitespace:** trimmed; files end with a newline

## Project Layout

```
src/
  main/
    kotlin/uk/gov/justice/digital/hmpps/
      config/         # Spring configuration classes
      resource/       # REST controllers (account, credit, disbursement,
                      #   payment, prison, security, transaction, etc.)
      service/        # Business logic services
      model/          # JPA entities
      repository/     # Spring Data JPA repositories
      dto/            # Data transfer objects / request-response models
    resources/
      application.yml           # default Spring config
      application-dev.yml       # local dev profile overrides
  test/
    kotlin/uk/gov/justice/digital/hmpps/
      integration/    # Integration tests (WebFlux test client, WireMock)
      unit/           # Unit tests
helm_deploy/          # Helm chart + values-{dev,preprod,prod}.yaml
build.gradle.kts      # Build configuration and dependencies
docker-compose.yml    # Local dev services (app, auth, database)
Dockerfile            # Multi-stage container build
```

## Testing Notes

- Unit tests use JUnit 5 and Mockito.
- Integration tests use `@SpringBootTest` with `WebTestClient` and WireMock for external service mocks.
- The HMPPS Kotlin test starter (`hmpps-kotlin-spring-boot-starter-test`) provides common test utilities.
- CI runs tests via `./gradlew assemble check` in GitHub Actions.

## Spring Profiles

- **dev** — local development; activated via `SPRING_PROFILES_ACTIVE=dev` in Docker Compose or IDE run config.
- **prod** — production defaults in `application.yml`; environment-specific values injected via Helm.

## Key URLs (local dev)

- API: http://localhost:8080/
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health check: http://localhost:8080/health
- HMPPS Auth: http://localhost:8090/auth

## CI/CD

- GitHub Actions pipeline: lint, test, build Docker image, push to `ghcr.io/ministryofjustice`, deploy via Helm.
- Security scanning: CodeQL, OWASP dependency checks, Trivy container scans, Veracode.
- Docker image registry: `ghcr.io/ministryofjustice/hmpps-money-to-prisoners-api`

## Key Dependencies

- `hmpps-kotlin-spring-boot-starter` — HMPPS common Spring Boot configuration
- `springdoc-openapi-starter-webmvc-ui` — OpenAPI/Swagger documentation
- Spring Boot WebFlux — reactive HTTP client for inter-service calls
- Spring Data JPA + PostgreSQL — database access