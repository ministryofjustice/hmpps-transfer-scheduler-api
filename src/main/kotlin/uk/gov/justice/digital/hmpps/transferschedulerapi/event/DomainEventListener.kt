package uk.gov.justice.digital.hmpps.transferschedulerapi.event

import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.PersonUpdatedHandler

@Component
class DomainEventListener(
  private val jsonMapper: JsonMapper,
  private val personUpdated: PersonUpdatedHandler,
) {

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun handleDomainEvent(notification: Notification) {
    try {
      when (notification.eventType) {
        PrisonerUpdated.EVENT_TYPE -> personUpdated.handle(jsonMapper.readValue(notification.message))
      }
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      SchedulerContext.clear()
    }
  }
}
