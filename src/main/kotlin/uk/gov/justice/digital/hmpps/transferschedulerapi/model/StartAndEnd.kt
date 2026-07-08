package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.Temporal
import kotlin.reflect.KClass

interface StartAndEnd<T : Temporal> {
  val start: T?
  val end: T?
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [StartAndEndValidator::class])
annotation class ValidStartAndEnd(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "End must be after start."
  }
}

class StartAndEndValidator : ConstraintValidator<ValidStartAndEnd, StartAndEnd<*>> {
  override fun isValid(request: StartAndEnd<*>, context: ConstraintValidatorContext): Boolean = with(request) {
    return if (start == null || end == null) {
      true
    } else {
      when (start) {
        is LocalDate -> !Period.between(start as LocalDate, end as LocalDate).isNegative
        is LocalDateTime -> Duration.between(start, end).isPositive
        else -> throw UnsupportedOperationException("${start!!::class.simpleName} is not supported by this validator")
      }
    }
  }
}
