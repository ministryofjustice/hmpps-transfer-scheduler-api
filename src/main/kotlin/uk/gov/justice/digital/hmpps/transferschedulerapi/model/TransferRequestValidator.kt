package uk.gov.justice.digital.hmpps.transferschedulerapi.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TransferRequestValidator::class])
annotation class ValidTransferRequest(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "Either a plan or schedule must be specified."
  }
}

class TransferRequestValidator : ConstraintValidator<ValidTransferRequest, TransferRequest> {
  override fun isValid(request: TransferRequest, context: ConstraintValidatorContext): Boolean = request.plan != null || request.schedule != null
}
