package uk.gov.justice.digital.hmpps.transferschedulerapi.event

import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationUrlBuilder.transferUrl
import java.util.UUID

data class TransferMovementMigrated(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.migrated"
    const val DESCRIPTION = "A transfer movement has been migrated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementMigrated(
      TransferInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementRecorded(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.recorded"
    const val DESCRIPTION = "A transfer movement has been recorded"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementRecorded(
      TransferInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementLogisticsChanged(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.logistics-changed"
    const val DESCRIPTION = "The logistics of a transfer movement have changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementLogisticsChanged(
      TransferInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementRecategorised(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.recategorised"
    const val DESCRIPTION = "The reason for a transfer movement has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementRecategorised(
      TransferInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementRelocated(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.relocated"
    const val DESCRIPTION = "The destination of a transfer movement has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementRelocated(
      TransferInformation(id, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
