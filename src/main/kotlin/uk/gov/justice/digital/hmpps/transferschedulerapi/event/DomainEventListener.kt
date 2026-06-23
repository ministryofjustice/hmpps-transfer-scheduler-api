package uk.gov.justice.digital.hmpps.transferschedulerapi.event

import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext

@Component
class DomainEventListener(
  private val jsonMapper: JsonMapper,
) {

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun handleDomainEvent(notification: Notification) {
    try {
      LOG.info("Received ${notification.eventType}")
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      SchedulerContext.clear()
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }
}
