package uk.gov.justice.digital.hmpps.transferschedulerapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.ServiceConfig

@EnableScheduling
@EnableConfigurationProperties(ServiceConfig::class)
@SpringBootApplication
class TransferSchedulerApi

fun main(args: Array<String>) {
  runApplication<TransferSchedulerApi>(*args)
}
