package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Persistable
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "migration_system_audit")
class MigrationSystemAudit(
  @Id
  @Column("id")
  val uuid: UUID,
  @Column(name = "created_at", nullable = false)
  var createdAt: LocalDateTime,
  @Column(name = "created_by", nullable = false)
  var createdBy: String,
  @Column(name = "modified_at")
  var modifiedAt: LocalDateTime?,
  @Column(name = "modified_by")
  var modifiedBy: String?,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "legacy_data")
  var data: LegacyData?,
) : Persistable<UUID> {
  override fun getId() = uuid

  @Transient
  var new: Boolean = true
  override fun isNew() = new

  @PostLoad
  fun onLoad() {
    new = false
  }
}

data class LegacyData(
  val waitList: WaitList?,
  val schedule: Schedule?,
) {
  data class WaitList(
    val statusDate: LocalDate,
    val approved: Boolean,
    val approvedStaffId: String?,
    val outcomeReasonCode: String?,
  )

  data class Schedule(
    val hiddenCommentText: String?,
    val outcomeReasonCode: String?,
  ) {
    init {
      check(hiddenCommentText != null || outcomeReasonCode != null) { "Invalid legacy data schedule object" }
    }
  }

  init {
    check(waitList != null || schedule != null) { "Invalid legacy data object" }
  }
}

interface MigrationSystemAuditRepository : CrudRepository<MigrationSystemAudit, UUID>
