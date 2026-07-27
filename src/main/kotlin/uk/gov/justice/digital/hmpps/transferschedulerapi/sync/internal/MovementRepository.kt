package uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import java.util.UUID

interface MovementRepository : JpaRepository<Movement, UUID> {
  fun findByLegacyId(legacyId: String): Movement?
}
