package uk.gov.justice.digital.hmpps.transferschedulerapi.model.paged

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.by
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
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

  val statusCodes: Set<TransferStatus.Code>
  val reasonCodes: Set<String>

  override fun validSortFields(): Set<String> = setOf(REASON, STATUS)

  override fun buildSort(field: String, direction: Direction): Sort = when (field) {
    PLAN_REQUESTED -> by(direction, "${PLAN}_${PLAN_REQUESTED}").and(sortByPersonIdentifier())
    SCHEDULE_START -> by(direction, "${SCHEDULE}_${SCHEDULE_START}").and(sortByPersonIdentifier())
    STATUS -> by(direction, "${field}_${SEQUENCE_NUMBER}").and(sortByPersonIdentifier())
    REASON -> by(direction, "${field}_description").and(sortByPersonIdentifier())
    else -> throw IllegalArgumentException("Unrecognised sort field")
  }

  private fun sortByPersonIdentifier(direction: Direction = Direction.ASC) = by(direction, "${PERSON}_$IDENTIFIER")

  companion object {
    internal val PLAN = Transfer::plan.name
    internal val PLAN_REQUESTED = Plan::requestedOn.name
    internal val SCHEDULE = Transfer::schedule.name
    internal val SCHEDULE_START = Schedule::start.name
    internal val REASON = Transfer::reason.name
    internal val STATUS = Transfer::status.name
    internal val PERSON = Transfer::person.name
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
    return start == null &&
      end == null ||
      !(start == null || end == null) &&
      when (start) {
        is LocalDate -> DAYS.between(start as LocalDate, end as LocalDate) <= daysBetween
        is LocalDateTime -> Duration.between(start, end).toDays() <= daysBetween
        else -> throw UnsupportedOperationException("${start!!::class.simpleName} is not supported by this validator")
      }
  }
}
