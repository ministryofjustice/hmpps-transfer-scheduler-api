package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import io.sentry.Sentry
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ExpireTransfer
import java.time.LocalDate

@Transactional
@Service
class TransferExpirer(
  private val rdRepository: ReferenceDataRepository,
  private val transferRepository: TransferRepository,
) {
  fun expireScheduledTransfers() {
    val rdProvider = rdRepository.rdProvider()
    val scheduled = rdProvider.get<TransferStatus>(TransferStatus.Code.SCHEDULED.name)
    transferRepository.findByStatusIdAndScheduleStartBefore(scheduled.id, LocalDate.now().atStartOfDay())
      .forEach { it.expire(ExpireTransfer, rdProvider) }
  }
}

@Conditional(PollExpiredTransferCondition::class)
@Service
class AppearanceExpiringPoller(private val transferExpirer: TransferExpirer) {
  @Scheduled(cron = $$"${service.transfer-expiration.cron}")
  fun recalculatePastAppearances() {
    try {
      RetryTemplate().apply {
        setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = 3 })
        setBackOffPolicy(ExponentialBackOffPolicy().apply { initialInterval = 1000L })
      }.execute<Unit, RuntimeException> {
        transferExpirer.expireScheduledTransfers()
      }
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      SchedulerContext.clear()
    }
  }
}

class PollExpiredTransferCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<String>("service.transfer-expiration.cron", "").isNotBlank()
}
