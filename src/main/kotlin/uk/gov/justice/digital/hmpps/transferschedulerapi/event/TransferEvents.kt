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

data class TransferCancelled(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.cancelled"
    const val DESCRIPTION = "A transfer has been cancelled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferCancelled(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferExpired(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.expired"
    const val DESCRIPTION = "A transfer has expired"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferExpired(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferCompleted(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.completed"
    const val DESCRIPTION = "A transfer has been completed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferCompleted(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferRecategorised(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.recategorised"
    const val DESCRIPTION = "The reason for a transfer has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferRecategorised(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferRelocated(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.relocated"
    const val DESCRIPTION = "The destination of a transfer has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferRelocated(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferLogisticsChanged(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.logistics-changed"
    const val DESCRIPTION = "The logistics of a transfer have changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferLogisticsChanged(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovedToPlanning(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.moved-to-planning"
    const val DESCRIPTION = "A scheduled transfer has been moved to planning"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovedToPlanning(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferInTransit(
  override val additionalInformation: TransferAppearanceInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferAppearanceInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.in-transit"
    const val DESCRIPTION = "A transfer is in transit"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferInTransit(
      TransferAppearanceInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
