package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEventRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository

@TestConfiguration
class TestConfig(
  private val transactionTemplate: TransactionTemplate,
  private val hmppsDomainEventRepository: HmppsDomainEventRepository,
  private val personSummaryRepository: PersonSummaryRepository,
  private val transferRepository: TransferRepository,
) {
  @Bean
  fun hmppsDomainEventOperations(): HmppsDomainEventOperations = HmppsDomainEventOperationsImpl(transactionTemplate, hmppsDomainEventRepository)

  @Bean
  fun personSummaryOperations(): PersonSummaryOperations = PersonSummaryOperationsImpl(personSummaryRepository)

  @Bean
  fun transferOperations(): TransferOperations = TransferOperationsImpl(transferRepository)
}
