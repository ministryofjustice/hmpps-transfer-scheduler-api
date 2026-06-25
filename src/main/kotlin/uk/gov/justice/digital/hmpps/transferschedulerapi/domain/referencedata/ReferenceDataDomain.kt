package uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata

import jakarta.persistence.Cacheable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import kotlin.reflect.KClass

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
@Entity
@Table(name = "reference_data_domain")
class ReferenceDataDomain(
  @Id
  @Enumerated(EnumType.STRING)
  val code: Code,
  val description: String,
) {
  enum class Code(val clazz: KClass<out ReferenceData>) {
    TRANSFER_LOGISTICS(TransferLogistics::class),
    TRANSFER_PRIORITY(TransferPriority::class),
    TRANSFER_REASON(TransferReason::class),
    TRANSFER_STATUS(TransferStatus::class),
    ;

    companion object {
      fun of(domain: String): Code = entries.firstOrNull {
        it.name.lowercase().replace("_", "") == domain.lowercase().replace("[_|-]".toRegex(), "")
      } ?: throw NotFoundException("Reference data domain not found")
    }
  }
}

interface ReferenceDataDomainRepository : JpaRepository<ReferenceDataDomain, ReferenceDataDomain.Code>

fun ReferenceDataDomainRepository.getDomain(code: ReferenceDataDomain.Code): ReferenceDataDomain = findByIdOrNull(code) ?: throw NotFoundException("Reference data domain not found")
