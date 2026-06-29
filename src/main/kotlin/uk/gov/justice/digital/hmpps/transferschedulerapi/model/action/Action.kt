package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

interface Action<T> {
  fun apply(entity: T)
}
