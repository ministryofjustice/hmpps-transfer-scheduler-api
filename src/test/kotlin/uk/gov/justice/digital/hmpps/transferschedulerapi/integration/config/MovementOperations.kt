package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.internal.MovementRepository
import java.util.UUID

interface MovementOperations {
  fun findMovement(uuid: UUID): Movement?
}

class MovementTransferOperationsImpl(
  private val movementRepository: MovementRepository,
) : MovementOperations {
  override fun findMovement(uuid: UUID): Movement? = movementRepository.findByIdOrNull(uuid)
}
