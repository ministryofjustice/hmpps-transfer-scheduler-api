package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.RdProvider
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.DomainEvent

interface Action<T> {
  fun applyTo(entity: T, rdProvider: RdProvider)
  fun domainEvent(entity: T): DomainEvent<*>? = null
}
