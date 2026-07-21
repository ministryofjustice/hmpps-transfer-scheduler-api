package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
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
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementMigrated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement.ApplyDestination
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement.ApplyLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement.ApplyReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement.MovementAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.StringLegacyIdRequest
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "movement")
final class Movement(
  transfer: Transfer,
  occurredAt: LocalDateTime,
  destinationCode: String,
  reason: TransferReason,
  logistics: TransferLogistics,
  comments: String?,
  legacyId: String?,
  @Id
  @Column(name = "id", nullable = false)
  override val id: UUID = IdGenerator.newUuid(),
) : Identifiable,
  DomainEventProducer {

  @Version
  @Column(name = "version", nullable = false)
  override var version: Int? = null
    private set

  @MapsId
  @OneToOne
  @JoinColumn(name = "id", nullable = false)
  var transfer: Transfer = transfer
    private set

  @NotNull
  @Column(name = "occurred_at", nullable = false)
  var occurredAt: LocalDateTime = occurredAt
    private set

  @Size(max = 6)
  @Column(name = "destination_code", length = 6, nullable = false)
  var destinationCode: String = destinationCode
    private set

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "reason_id", nullable = false)
  var reason: TransferReason = reason
    private set

  @Fetch(FetchMode.JOIN)
  @Audited(targetAuditMode = NOT_AUDITED)
  @ManyToOne
  @JoinColumn(name = "logistics_id", nullable = false)
  var logistics: TransferLogistics = logistics
    private set

  @Column(name = "comments")
  var comments: String? = comments
    private set

  @Size(max = 32)
  @Column(name = "legacy_id", length = 32)
  var legacyId: String? = legacyId
    private set

  @Transient
  private var appliedActions: List<MovementAction> = listOf()

  @PostLoad
  private fun load() {
    appliedActions = listOf()
  }

  override fun initialEvents(): Set<DomainEventPublication> = if (SchedulerContext.get().migratingData) {
    setOf(TransferMovementMigrated(transfer.person.identifier, id).publication(id))
  } else {
    setOf(TransferMovementRecorded(transfer.person.identifier, id).publication(id))
  }

  override fun domainEvents(): Set<DomainEventPublication> = appliedActions.mapNotNull {
    it.domainEvent(this)?.publication(id)
  }.toSet()

  fun match(request: MovementRequest, rdProvider: RdProvider) = apply {
    occurredAt = request.occurredAt
    applyDestination(ApplyDestination(request.destinationCode))
    applyReason(ApplyReason(request.reasonCode), rdProvider)
    applyLogistics(ApplyLogistics(request.logisticsCode), rdProvider)
    comments = request.comments
    if (request is StringLegacyIdRequest) {
      legacyId = request.legacyId
    }
  }

  fun applyDestination(action: ApplyDestination) = apply {
    if (destinationCode != action.destinationCode) {
      destinationCode = action.destinationCode
      appliedActions += action
    }
  }

  fun applyReason(action: ApplyReason, rdProvider: RdProvider) = apply {
    if (reason.code != action.reasonCode) {
      reason = rdProvider.get(action.reasonCode)
      appliedActions += action
    }
  }

  fun applyLogistics(action: ApplyLogistics, rdProvider: RdProvider) = apply {
    if (logistics.code != action.logisticsCode) {
      logistics = rdProvider.get(action.logisticsCode)
      appliedActions += action
    }
  }

  companion object {
    fun auditedProperties() = listOf(
      Movement::occurredAt,
      Movement::comments,
    )
  }
}
