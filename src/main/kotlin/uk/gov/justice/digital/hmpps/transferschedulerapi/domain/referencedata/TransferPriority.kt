package uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Cacheable
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import java.util.UUID

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
@Entity
@Table(name = "transfer_priority")
class TransferPriority(
  code: String,
  description: String,
  sequenceNumber: Int,
  active: Boolean,
  id: UUID,
) : ReferenceData(code, description, sequenceNumber, active, id) {
  enum class Code(@JsonValue val value: String) {
    HIGH("1"),
    MEDIUM("2"),
    LOW("3"),
    ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun fromValue(value: String): Code? = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
  }
}
