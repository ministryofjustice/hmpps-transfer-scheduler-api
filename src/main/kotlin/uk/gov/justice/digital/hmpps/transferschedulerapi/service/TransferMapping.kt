package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PrisonProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.CancelledRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.IdRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.IntegrationResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Person
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PrisonRelatedRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.NumericLegacyIdRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.MigrationSystemAudit
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.MigrationSystemAuditRepository
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement as MovementEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan as PlanEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule as ScheduleEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer as TransferEntity

fun TransferRequest.asEntity(
  person: PersonSummary,
  rdProvider: RdProvider,
  generateDefaultPlan: Boolean,
) = TransferEntity(
  person,
  if (this is PrisonRelatedRequest) prisonCode else requireNotNull(person.prisonCode),
  rdProvider.get(reasonCode),
  rdProvider.get(initialStatusCode().name),
  destinationCode,
  logisticsCode?.let { rdProvider.get(it) },
  initialStage(),
  if (this is CancelledRequest && initialStatusCode() == TransferStatus.Code.CANCELLED) cancellationReason?.let { rdProvider.get(it) } else null,
  if (this is NumericLegacyIdRequest) legacyId else null,
  if (this is IdRequest) id else newUuid(),
)
  .withPlan(plan ?: if (generateDefaultPlan) scheduledPlan() else null, rdProvider)
  .withSchedule(schedule)

private fun scheduledPlan(): PlanRequest = object : PlanRequest {
  override val requestedOn: LocalDate = LocalDate.now()
  override val priorityCode: String = TransferPriority.Code.HIGH.value
  override val comments: String = "Plan generated automatically for reporting purposes. Transfer created as scheduled."
}

fun TransferEntity.asModel(prisonProvider: PrisonProvider) = Transfer(
  id,
  person(),
  prisonProvider(prisonCode),
  status.asCodedDescription(),
  reason.asCodedDescription(),
  destinationCode?.let { prisonProvider(it) },
  logistics?.asCodedDescription(),
  plan(),
  schedule(),
  movement(prisonProvider),
  stage,
)

private fun TransferEntity.person() = Person(person.identifier, person.firstName, person.lastName)

private fun TransferEntity.plan(): Plan? = with(plan) {
  this?.let { Plan(requestedOn, priority.asCodedDescription(), comments) }
}

private fun TransferEntity.schedule(): Schedule? = with(schedule) {
  this?.let { Schedule(start, comments) }
}

private fun TransferEntity.movement(prisonProvider: PrisonProvider): Movement? = with(movement) {
  this?.let {
    Movement(
      occurredAt,
      prisonProvider(destinationCode),
      reason.asCodedDescription(),
      logistics.asCodedDescription(),
      comments,
    )
  }
}

fun TransferEntity.forIntegration() = IntegrationResponse.Transfer(
  id,
  person.identifier,
  prisonCode,
  status.asCodedDescription(),
  reason.asCodedDescription(),
  destinationCode,
  logistics?.asCodedDescription(),
  plan?.forIntegration(),
  schedule?.forIntegration(),
  movement?.forIntegration(),
)

fun PlanEntity.forIntegration() = IntegrationResponse.Plan(requestedOn, priority.asCodedDescription(), comments)

fun ScheduleEntity.forIntegration() = IntegrationResponse.Schedule(start, comments)

fun MovementEntity.forIntegration() = IntegrationResponse.Movement(occurredAt, comments)

fun TransferEntity.recordApprovalInformation(msaRepository: MigrationSystemAuditRepository): TransferEntity = apply {
  if (plan == null) return@apply
  msaRepository.findByIdOrNull(id)?.approvedScheduled(this) ?: run {
    val context = SchedulerContext.get()
    msaRepository.save(
      MigrationSystemAudit(
        id,
        context.requestAt,
        context.username,
        null,
        null,
        null,
      ).approvedScheduled(this),
    )
  }
}
