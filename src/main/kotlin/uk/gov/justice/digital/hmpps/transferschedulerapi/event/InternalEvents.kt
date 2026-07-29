package uk.gov.justice.digital.hmpps.transferschedulerapi.event

import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import java.util.UUID

data class InternalEvents(val toPublish: List<DomainEvent<*>>) {
  constructor(de: DomainEvent<*>) : this(listOf(de))
}

data class InternalInformation(
  override val id: UUID,
  override val source: DataSource,
) : AdditionalInformation,
  IdInformation,
  SourceInformation

data class PlanningIncomplete(
  override val additionalInformation: InternalInformation,
  override val personReference: PersonReference,
) : DomainEvent<InternalInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String? = null

  companion object {
    const val EVENT_TYPE = "internal.transfer.planning-incomplete"
    const val DESCRIPTION = "A transfer plan is incomplete"
    operator fun invoke(
      personIdentifier: String,
      transferId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = PlanningIncomplete(
      InternalInformation(transferId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
