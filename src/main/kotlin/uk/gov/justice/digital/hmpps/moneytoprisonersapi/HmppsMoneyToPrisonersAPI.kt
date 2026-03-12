package uk.gov.justice.digital.hmpps.moneytoprisonersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsMoneyToPrisonersAPI

fun main(args: Array<String>) {
  runApplication<HmppsMoneyToPrisonersAPI>(*args)
}
