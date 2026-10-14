package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.TransferExpirer
import java.time.LocalDate
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

  @Test
  fun `can set ready to schedule as awaiting details`() {
    val today = givenTransfer(
      transfer(
        schedule = schedule(start = LocalDate.now().atStartOfDay()),
        statusCode = TransferStatus.Code.READY_TO_SCHEDULE,
      ),
    )
    assertThat(today.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)
    val yesterday = givenTransfer(
      transfer(
        schedule = schedule(start = LocalDateTime.now().minusDays(1)),
        statusCode = TransferStatus.Code.READY_TO_SCHEDULE,
      ),
    )
    assertThat(yesterday.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)

    transferExpirer.expireScheduledTransfers()

    val rts = requireNotNull(findTransfer(today.id))
    assertThat(rts.status.code).isEqualTo(TransferStatus.Code.READY_TO_SCHEDULE.name)

    val awaitingDetails = requireNotNull(findTransfer(yesterday.id))
    assertThat(awaitingDetails.status.code).isEqualTo(TransferStatus.Code.PLANNING.name)
  }
}
