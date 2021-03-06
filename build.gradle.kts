plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.4"
  kotlin("plugin.spring") version "1.5.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
