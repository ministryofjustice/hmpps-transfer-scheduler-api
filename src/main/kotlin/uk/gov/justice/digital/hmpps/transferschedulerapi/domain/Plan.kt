package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import java.time.LocalDate
import java.util.UUID

@Audited
@Entity
@Table(name = "plan")
final class Plan(
  transfer: Transfer,
  requestedOn: LocalDate,
  priority: TransferPriority,
  comments: String?,
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
  @OneToOne(optional = false)
  @JoinColumn(name = "id", nullable = false)
  var transfer: Transfer = transfer
    private set

  @NotNull
  @Column(name = "requested_on", nullable = false)
  var requestedOn: LocalDate = requestedOn
    private set

  @Audited(targetAuditMode = NOT_AUDITED)
  @NotNull
  @ManyToOne
  @JoinColumn(name = "priority_id", nullable = false)
  var priority: TransferPriority = priority
    private set

  @Column(name = "comments")
  var comments: String? = comments
    private set
}
