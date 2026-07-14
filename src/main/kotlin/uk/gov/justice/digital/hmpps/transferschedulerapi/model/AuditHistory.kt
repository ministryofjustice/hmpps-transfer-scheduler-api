package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AuditHistory(val content: List<AuditedAction>)

data class AuditedAction(
  val user: User,
  val occurredAt: LocalDateTime,
  val domainEvents: List<String>,
  val reason: String?,
  val changes: List<Change>,
) {
  data class User(val username: String, val name: String)
  data class Change(
    val propertyName: String,
    @field:Schema(
      oneOf = [
        String::class,
        Long::class,
        Double::class,
        Boolean::class,
        Map::class,
        List::class,
      ],
      nullable = true,
    ) val previous: Any?,
    @field:Schema(
      oneOf = [
        String::class,
        Long::class,
        Double::class,
        Boolean::class,
        Map::class,
        List::class,
      ],
      nullable = true,
    ) val change: Any?,
  )
}
