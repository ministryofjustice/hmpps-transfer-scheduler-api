package uk.gov.justice.digital.hmpps.transferschedulerapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TransferSchedulerApi

fun main(args: Array<String>) {
  runApplication<TransferSchedulerApi>(*args)
}
