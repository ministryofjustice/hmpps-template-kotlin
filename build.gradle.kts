plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.2"
  kotlin("plugin.spring") version "2.0.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.0.3")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.0.3")
  testImplementation("org.wiremock:wiremock-standalone:3.9.1")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
