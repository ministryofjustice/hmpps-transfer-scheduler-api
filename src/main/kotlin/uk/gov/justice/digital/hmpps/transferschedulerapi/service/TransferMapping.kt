package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PrisonProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Person
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.NumericLegacyIdRequest

fun TransferRequest.asEntity(person: PersonSummary, rdProvider: RdProvider) = uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer(
  person,
  requireNotNull(person.prisonCode),
  rdProvider.get(reasonCode),
  rdProvider.get(initialStatusCode().name),
  destinationCode,
  logisticsCode?.let { rdProvider.get<TransferLogistics>(it) },
  if (this is NumericLegacyIdRequest) legacyId else null,
)
  .withPlan(plan, rdProvider)
  .withSchedule(schedule)

fun uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer.asModel(prisonProvider: PrisonProvider) = Transfer(
  id,
  person(),
  prisonProvider(prisonCode),
  status.asCodedDescription(),
  reason.asCodedDescription(),
  destinationCode?.let { prisonProvider(it) },
  logistics?.asCodedDescription(),
  plan(),
  schedule(),
  movement(),
)

private fun uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer.person() = Person(person.identifier, person.firstName, person.lastName)

private fun uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer.plan(): Plan? = with(plan) {
  this?.let { Plan(requestedOn, priority.asCodedDescription(), comments) }
}

private fun uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer.schedule(): Schedule? = with(schedule) {
  this?.let { Schedule(start, comments) }
}

private fun uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer.movement(): Movement? = with(movement) {
  this?.let { Movement(occurredAt, comments) }
}
