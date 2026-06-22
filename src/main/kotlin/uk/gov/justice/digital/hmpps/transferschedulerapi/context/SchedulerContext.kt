package uk.gov.justice.digital.hmpps.transferschedulerapi.context

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import java.time.LocalDateTime

data class SchedulerContext(
  val username: String,
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val reason: String? = null,
  val source: DataSource = DataSource.DPS,
  val migratingData: Boolean = false,
  val caseloadId: String? = null,
) {
  companion object {
    const val SYSTEM_USERNAME = "SYS"

    fun get(): SchedulerContext = SchedulerContextHolder.getContext()
    fun clear() {
      SchedulerContextHolder.clearContext()
    }
  }
}

fun SchedulerContext.set() = apply { SchedulerContextHolder.setContext(this) }

@Component
class SchedulerContextHolder {
  companion object {
    private var context: ThreadLocal<SchedulerContext> =
      ThreadLocal.withInitial { SchedulerContext(SYSTEM_USERNAME) }

    internal fun getContext(): SchedulerContext = context.get()
    internal fun setContext(sc: SchedulerContext) {
      context.set(sc)
    }

    internal fun clearContext() {
      context.remove()
    }
  }
}
