package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import java.time.LocalDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = "schedule")
final class Schedule(
  transfer: Transfer,
  start: LocalDateTime,
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
  @Column(name = "start", nullable = false)
  var start: LocalDateTime = start
    private set

  @Column(name = "comments")
  var comments: String? = comments
    private set

  fun match(request: ScheduleRequest) = apply {
    start = request.start
    comments = request.comments
  }

  companion object {
    fun auditedProperties() = listOf(
      Schedule::start,
      Schedule::comments,
    )
  }
}
