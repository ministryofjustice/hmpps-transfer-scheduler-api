package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.movement

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.Action

sealed interface MovementAction : Action<Movement>
