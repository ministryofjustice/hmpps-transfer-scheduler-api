package uk.gov.justice.digital.hmpps.transferschedulerapi.sync

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [SyncTransferValidator::class])
annotation class ValidSyncTransfer(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "Either a schedule or movement must be specified."
  }
}

class SyncTransferValidator : ConstraintValidator<ValidSyncTransfer, SyncTransfer> {
  override fun isValid(request: SyncTransfer, context: ConstraintValidatorContext): Boolean = with(request) {
    syncSchedule != null || syncMovement != null
  }
}
