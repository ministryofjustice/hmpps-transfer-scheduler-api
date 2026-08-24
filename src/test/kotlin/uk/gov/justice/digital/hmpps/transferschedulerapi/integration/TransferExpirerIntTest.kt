package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.TransferExpirer
import java.time.LocalDateTime

class TransferExpirerIntTest(
  @Autowired tro: TransferOperations,
  @Autowired val transferExpirer: TransferExpirer,
) : IntegrationTestBase(),
  TransferOperations by tro {

  @Test
  fun `can expire past scheduled transfers`() {
    val today = givenTransfer(transfer(plan = null))
    assertThat(today.status.code).isEqualTo(TransferStatus.Code.SCHEDULED.name)
    val yesterday = givenTransfer(transfer(plan = null, schedule = schedule(start = LocalDateTime.now().minusDays(1))))
    assertThat(yesterday.status.code).isEqualTo(TransferStatus.Code.SCHEDULED.name)

    transferExpirer.expireScheduledTransfers()

    val scheduled = requireNotNull(findTransfer(today.id))
    assertThat(scheduled.status.code).isEqualTo(TransferStatus.Code.SCHEDULED.name)

    val expired = requireNotNull(findTransfer(yesterday.id))
    assertThat(expired.status.code).isEqualTo(TransferStatus.Code.EXPIRED.name)
  }
}
