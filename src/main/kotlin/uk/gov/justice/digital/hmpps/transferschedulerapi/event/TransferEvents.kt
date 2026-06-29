package uk.gov.justice.digital.hmpps.transferschedulerapi.event

import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationUrlBuilder.transferUrl
import java.util.UUID

data class TransferAppearanceInformation(
  override val id: UUID,
  override val source: DataSource,
) : SourceInformation,
  IdInformation

data class TransferMigrated(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.migrated"
    const val DESCRIPTION = "A transfer has been migrated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMigrated(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferRecorded(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.recorded"
    const val DESCRIPTION = "A transfer has been recorded"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferRecorded(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferPlanned(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.planned"
    const val DESCRIPTION = "A transfer has been planned"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferPlanned(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferScheduled(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.scheduled"
    const val DESCRIPTION = "A transfer has been scheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferScheduled(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
