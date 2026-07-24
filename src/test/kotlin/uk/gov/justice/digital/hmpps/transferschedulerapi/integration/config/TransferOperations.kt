package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.TransferRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.PersonSummaryOperations.Companion.personSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferPriorityCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

typealias PersonProvider = (String, String) -> PersonSummary
typealias TransferProvider = (PersonProvider, RdProvider) -> Transfer

interface TransferOperations {
  fun findTransfer(uuid: UUID): Transfer?
  fun givenTransfer(tp: TransferProvider): Transfer
}

class TransferOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val rdRepository: ReferenceDataRepository,
  private val transferRepository: TransferRepository,
  private val psOperations: PersonSummaryOperations,
) : TransferOperations {
  override fun findTransfer(uuid: UUID): Transfer? = transferRepository.findByIdOrNull(uuid)
  override fun givenTransfer(tp: TransferProvider): Transfer = transactionTemplate.execute {
    transferRepository.save(
      tp(
        { personIdentifier, prisonCode ->
          psOperations.findPersonSummary(personIdentifier)
            ?: psOperations.givenPersonSummary(personSummary(personIdentifier, prisonCode = prisonCode))
        },
        rdRepository.rdProvider(),
      ),
    )
  }

  companion object {
    fun transfer(
      personIdentifier: String = personIdentifier(),
      prisonCode: String = prisonCode(),
      reasonCode: String? = TransferReasonCode.randomCode(),
      statusCode: TransferStatus.Code = TransferStatus.Code.SCHEDULED,
      destinationCode: String? = prisonCode(),
      logisticsCode: String? = TransferLogisticsCode.randomCode(),
      plan: PlanRequest? = plan(),
      schedule: ScheduleRequest? = schedule(),
      movement: MovementRequest? = null,
      stage: TransferStage = if (plan == null && schedule == null) {
        TransferStage.UNSCHEDULED
      } else {
        when (statusCode) {
          TransferStatus.Code.SCHEDULED, TransferStatus.Code.IN_TRANSIT, TransferStatus.Code.COMPLETED -> TransferStage.SCHEDULED
          TransferStatus.Code.PLANNING, TransferStatus.Code.READY_TO_SCHEDULE -> TransferStage.PLANNING
          else -> throw IllegalStateException("No default for transfer status $statusCode")
        }
      },
      legacyId: Long? = null,
      id: UUID = newUuid(),
    ): TransferProvider = { pp, rd ->
      Transfer(
        pp(personIdentifier, prisonCode),
        prisonCode,
        rd.get(requireNotNull(reasonCode ?: movement?.reasonCode) { "Reason must be provided" }),
        rd.get(statusCode.name),
        destinationCode ?: movement?.destinationCode,
        (logisticsCode ?: movement?.logisticsCode)?.let { rd.get(it) },
        stage,
        legacyId,
        id,
      )
        .withPlan(plan, rd)
        .withSchedule(schedule)
        .withMovement(movement, rd)
    }

    fun plan(
      requestedOn: LocalDate = LocalDate.now(),
      priorityCode: String = TransferPriorityCode.randomCode(),
      comments: String? = word(30),
    ) = object : PlanRequest {
      override val requestedOn: LocalDate = requestedOn
      override val priorityCode: String = priorityCode
      override val comments: String? = comments
    }

    fun schedule(
      start: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      comments: String? = word(30),
    ) = object : ScheduleRequest {
      override val start: LocalDateTime = start
      override val comments: String? = comments
    }

    fun movement(
      occurredAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
      destinationCode: String = prisonCode(),
      reasonCode: String = TransferReasonCode.randomCode(),
      logisticsCode: String = TransferLogisticsCode.randomCode(),
      comments: String? = word(30),
    ) = object : MovementRequest {
      override val occurredAt: LocalDateTime = occurredAt
      override val destinationCode: String = destinationCode
      override val reasonCode: String = reasonCode
      override val logisticsCode: String = logisticsCode
      override val comments: String? = comments
    }
  }
}
