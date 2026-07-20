package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementMigrated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferMovementRecorded
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.MovementRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.TransferAction
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.StringLegacyIdRequest
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "movement")
final class Movement(
  transfer: Transfer,
  occurredAt: LocalDateTime,
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
  @JoinColumn(name = "id")
  var transfer: Transfer = transfer
    private set

  @NotNull
  @Column(name = "occurred_at", nullable = false)
  var occurredAt: LocalDateTime = occurredAt
    private set

  @Column(name = "comments")
  var comments: String? = comments
    private set

  @Size(max = 32)
  @Column(name = "legacy_id", length = 32)
  var legacyId: String? = legacyId
    private set

  @Transient
  private var appliedActions: List<TransferAction> = listOf()

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
    it.domainEvent(transfer)?.publication(id)
  }.toSet()

  fun match(request: MovementRequest) = apply {
    occurredAt = request.occurredAt
    comments = request.comments
    if (request is StringLegacyIdRequest) {
      legacyId = request.legacyId
    }
  }

  companion object {
    fun auditedProperties() = listOf(
      Movement::occurredAt,
      Movement::comments,
    )
  }
}
