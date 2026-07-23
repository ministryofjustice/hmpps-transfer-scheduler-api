package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.COMPLETED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.IN_TRANSIT
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.PLANNING
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.READY_TO_SCHEDULE
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferDeleted
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMigrated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferPlanned
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ApplyTransit
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.CancelTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.CompleteTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ExpireTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.PlanTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.ScheduleTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.transfer.TransferAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.StringLegacyIdRequest
import java.util.UUID

@NamedEntityGraph(
  name = "transfer.all",
  attributeNodes = [
    NamedAttributeNode("person"),
    NamedAttributeNode("status"),
    NamedAttributeNode("reason"),
    NamedAttributeNode("logistics"),
    NamedAttributeNode("plan", subgraph = "transfer.plan"),
    NamedAttributeNode("schedule"),
    NamedAttributeNode("movement", subgraph = "transfer.movement"),
  ],
  subgraphs = [
    NamedSubgraph(name = "transfer.plan", attributeNodes = [NamedAttributeNode("priority")]),
    NamedSubgraph(
      name = "transfer.movement",
      attributeNodes = [NamedAttributeNode("reason"), NamedAttributeNode("logistics")],
    ),
  ],
)
@Audited
@Entity
@Table(name = "transfer")
final class Transfer(
  person: PersonSummary,
  prisonCode: String,
  reason: TransferReason?,
  status: TransferStatus,
  destinationCode: String?,
  logistics: TransferLogistics?,
  stage: TransferStage,
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
      field = StatusValidator(this) valid value
    }

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "reason_id")
  var reason: TransferReason? = reason
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
    private set

  @NotAudited
  @OneToOne(mappedBy = "transfer", cascade = [CascadeType.ALL])
  var schedule: Schedule? = null
    private set

  @NotAudited
  @OneToOne(mappedBy = "transfer", cascade = [CascadeType.ALL])
  var movement: Movement? = null
    private set

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType::class)
  @Column(name = "stage", columnDefinition = "transfer_stage", nullable = false)
  var stage: TransferStage = stage
    private set

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
      PLANNING, READY_TO_SCHEDULE -> TransferPlanned(person.identifier, id)
      SCHEDULED -> TransferScheduled(person.identifier, id)
      else -> TransferRecorded(person.identifier, id)
    }
    setOf(event.publication(id))
  }

  override fun domainEvents(): Set<DomainEventPublication> = appliedActions.mapNotNull {
    it.domainEvent(this)?.publication(id)
  }.toSet()

  override fun deletionEvents(): Set<DomainEventPublication> = setOf(
    TransferDeleted(person.identifier, id).publication(id),
  )

  fun isReadyToSchedule(): Boolean = plan != null && logistics != null && destinationCode != null && schedule != null

  fun withPlan(request: PlanRequest?, rdProvider: RdProvider) = apply {
    plan = request?.let { plan?.match(it, rdProvider) ?: it.createNewPlan(this, rdProvider) }
  }

  fun withSchedule(request: ScheduleRequest?) = apply {
    schedule = request?.let { schedule?.match(it) ?: it.createNewSchedule(this) }
  }

  fun withMovement(request: MovementRequest?, rdProvider: RdProvider) = apply {
    movement = request?.let { movement?.match(request, rdProvider) ?: it.createNewMovement(this, rdProvider) }
  }

  fun movePerson(person: PersonSummary, prisonCode: String) = apply {
    this.person = person
    this.prisonCode = prisonCode
  }

  fun applyDestination(action: ApplyDestination) = apply {
    if (destinationCode != action.destinationCode) {
      destinationCode = action.destinationCode
      appliedActions += action
    }
  }

  fun applyReason(action: ApplyReason, rdProvider: RdProvider) = apply {
    if (reason?.code != action.reasonCode) {
      reason = action.reasonCode?.let { rdProvider.get(it) }
      appliedActions += action
    }
  }

  fun applyLogistics(action: ApplyLogistics, rdProvider: RdProvider) = apply {
    if (logistics?.code != action.logisticsCode) {
      logistics = action.logisticsCode?.let { rdProvider.get(it) }
      appliedActions += action
    }
  }

  fun applyPlan(action: PlanTransfer, rdProvider: RdProvider) = apply {
    if (action changes plan) {
      withPlan(action, rdProvider)
    }
    if (applyStatus(READY_TO_SCHEDULE, rdProvider)) {
      stage = TransferStage.PLANNING
      appliedActions += action
    }
  }

  fun applySchedule(action: ScheduleTransfer, rdProvider: RdProvider) = apply {
    if (action changes schedule) {
      withSchedule(action)
    }
    if (applyStatus(SCHEDULED, rdProvider)) {
      stage = TransferStage.SCHEDULED
      appliedActions += action
    }
  }

  fun applyTransit(action: ApplyTransit, rdProvider: RdProvider) = apply {
    if (action changes movement) {
      withMovement(action, rdProvider)
      if (applyStatus(IN_TRANSIT, rdProvider)) {
        if (schedule == null) {
          stage = TransferStage.UNSCHEDULED
        }
        appliedActions += action
      }
    }
  }

  fun complete(action: CompleteTransfer, rdProvider: RdProvider) = apply {
    if (applyStatus(COMPLETED, rdProvider)) {
      if (schedule == null) {
        stage = TransferStage.UNSCHEDULED
      }
      appliedActions += action
    }
  }

  fun cancel(action: CancelTransfer, rdProvider: RdProvider) = apply {
    if (applyStatus(CANCELLED, rdProvider)) {
      appliedActions += action
    }
  }

  fun expire(action: ExpireTransfer, rdProvider: RdProvider) = apply {
    if (applyStatus(EXPIRED, rdProvider)) {
      appliedActions += action
    }
  }

  fun applyStatus(code: TransferStatus.Code, rdProvider: RdProvider): Boolean {
    val statusChange = status.code != code.name
    if (statusChange) {
      status = rdProvider.get(code.name)
    }
    return statusChange
  }

  fun applyLegacyId(legacyId: Long?) = apply {
    this.legacyId = legacyId
  }

  private fun PlanRequest.createNewPlan(transfer: Transfer, rdProvider: RdProvider) = Plan(transfer, requestedOn, rdProvider.get(priorityCode), comments)

  private fun ScheduleRequest.createNewSchedule(transfer: Transfer) = Schedule(transfer, start, comments)

  private fun MovementRequest.createNewMovement(transfer: Transfer, rdProvider: RdProvider) = Movement(
    transfer,
    occurredAt,
    destinationCode,
    rdProvider.get(reasonCode),
    rdProvider.get(logisticsCode),
    comments,
    if (this is StringLegacyIdRequest) legacyId else null,
  )

  companion object {
    fun auditedProperties() = listOf(
      Transfer::reason,
      Transfer::status,
      Transfer::destinationCode,
      Transfer::logistics,
    )
  }
}
