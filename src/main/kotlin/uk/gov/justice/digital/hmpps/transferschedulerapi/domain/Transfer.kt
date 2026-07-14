package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMigrated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferPlanned
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.TransferAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.StringLegacyIdRequest
import java.util.UUID

@Audited
@Entity
@Table(name = "transfer")
final class Transfer(
  person: PersonSummary,
  prisonCode: String,
  reason: TransferReason,
  status: TransferStatus,
  destinationCode: String?,
  logistics: TransferLogistics?,
  legacyId: Long?,
  @Id
  @Column(name = "id", nullable = false)
  override val id: UUID = newUuid(),
) : Identifiable,
  DomainEventProducer {

  @Version
  @Column(name = "version", nullable = false)
  override var version: Int? = null
    private set

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "person_identifier", nullable = false)
  var person: PersonSummary = person
    private set

  @Size(max = 6)
  @NotNull
  @Column(name = "prison_code", nullable = false, length = 6)
  var prisonCode: String = prisonCode
    private set

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "status_id", nullable = false)
  var status: TransferStatus = status
    private set(value) {
      if (value == field) return
      when (value.code) {
        READY_TO_SCHEDULE.name -> check(listOfNotNull(destinationCode, logistics, plan).isNotEmpty())
      }
      field = value
    }

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "reason_id", nullable = false)
  var reason: TransferReason = reason
    private set

  @Size(max = 6)
  @Column(name = "destination_code", length = 6)
  var destinationCode: String? = destinationCode
    private set

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "logistics_id")
  var logistics: TransferLogistics? = logistics
    private set

  @Column(name = "legacy_id")
  var legacyId: Long? = legacyId
    private set

  @NotAudited
  @OneToOne(mappedBy = "transfer", cascade = [CascadeType.ALL])
  var plan: Plan? = null

  @NotAudited
  @OneToOne(mappedBy = "transfer", cascade = [CascadeType.ALL])
  var schedule: Schedule? = null

  @NotAudited
  @OneToOne(mappedBy = "transfer", cascade = [CascadeType.ALL])
  var movement: Movement? = null

  @Transient
  private var appliedActions: List<TransferAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun initialEvents(): Set<DomainEventPublication> = if (SchedulerContext.get().migratingData) {
    setOf(TransferMigrated(person.identifier, id).publication(id))
  } else {
    val event = when (TransferStatus.Code.valueOf(status.code)) {
      TransferStatus.Code.PLANNING, READY_TO_SCHEDULE -> TransferPlanned(person.identifier, id)
      TransferStatus.Code.SCHEDULED -> TransferScheduled(person.identifier, id)
      else -> TransferRecorded(person.identifier, id)
    }
    setOf(event.publication(id))
  }

  fun withPlan(request: PlanRequest?, rdProvider: RdProvider) = apply {
    plan = request?.let {
      Plan(
        this,
        request.requestedOn,
        rdProvider.get<TransferPriority>(request.priorityCode),
        request.comments,
      )
    }
  }

  fun withSchedule(request: ScheduleRequest?) = apply {
    schedule = request?.let { Schedule(this, request.start, request.comments) }
  }

  fun withMovement(request: MovementRequest?) = apply {
    val slid = if (request is StringLegacyIdRequest) request.legacyId else null
    movement = request?.let { Movement(this, request.occurredAt, request.comments, slid) }
  }

  companion object {
    fun auditedProperties() = listOf(
      Transfer::reason,
      Transfer::status,
      Transfer::destinationCode,
      Transfer::logistics,
    )
  }
}
