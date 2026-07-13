package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceData.Companion.SEQUENCE_NUMBER
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.StartAndEnd
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.DAYS
import kotlin.properties.Delegates
import kotlin.reflect.KClass

interface TransferSearchRequest :
  PagedRequest,
  StartAndEnd<LocalDate> {

  @get:Schema(requiredMode = NOT_REQUIRED)
  val statusCodes: Set<TransferStatus.Code>
    get() = emptySet()

  @get:Schema(requiredMode = NOT_REQUIRED)
  val reasonCodes: Set<String>
    get() = emptySet()

  override fun validSortFields(): Set<String> = setOf(SCHEDULE_START, FIRST_NAME, LAST_NAME, REASON, STATUS)

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    LAST_NAME -> sortByPersonName(direction)
    FIRST_NAME -> sortByPersonName(direction, PERSON_FIRST_NAME, PERSON_LAST_NAME)
    SCHEDULE_START -> by(direction, "${SCHEDULE}_${SCHEDULE_START}").and(sortByPersonName())
    STATUS -> by(direction, "${field}_${SEQUENCE_NUMBER}").and(sortByPersonName())
    REASON -> by(direction, "${field}_description").and(sortByPersonName())

    else -> throw IllegalArgumentException("Unrecognised sort field")
  }

  private fun sortByPersonName(
    direction: Direction = Direction.ASC,
    first: String = PERSON_LAST_NAME,
    second: String = PERSON_FIRST_NAME,
  ) = by(
    direction,
    first,
    second,
    "${PERSON}_$IDENTIFIER",
  )

  companion object {
    internal const val FROM = "from"
    internal val SCHEDULE = Transfer::schedule.name
    internal val SCHEDULE_START = Schedule::start.name
    internal val REASON = Transfer::reason.name
    internal val STATUS = Transfer::status.name
    internal val PERSON = Transfer::person.name
    internal val PERSON_LAST_NAME = "${PERSON}_${LAST_NAME}"
    internal val PERSON_FIRST_NAME = "${PERSON}_${FIRST_NAME}"
  }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MonthBetweenValidator::class])
annotation class ValidDateRange(
  val daysBetween: Int = 31,
  val message: String = "Invalid date range",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class MonthBetweenValidator : ConstraintValidator<ValidDateRange, StartAndEnd<*>> {
  private var daysBetween by Delegates.notNull<Int>()

  override fun initialize(constraintAnnotation: ValidDateRange) {
    daysBetween = constraintAnnotation.daysBetween
  }

  override fun isValid(request: StartAndEnd<*>, context: ConstraintValidatorContext): Boolean = with(request) {
    return if (start == null || end == null) {
      false
    } else {
      when (start) {
        is LocalDate -> DAYS.between(start as LocalDate, end as LocalDate) <= daysBetween
        is LocalDateTime -> Duration.between(start, end).toDays() <= daysBetween
        else -> throw UnsupportedOperationException("${start!!::class.simpleName} is not supported by this validator")
      }
    }
  }
}
