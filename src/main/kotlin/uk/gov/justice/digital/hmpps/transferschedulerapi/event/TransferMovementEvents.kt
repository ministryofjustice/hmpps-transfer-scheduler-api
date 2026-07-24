package uk.gov.justice.digital.hmpps.transferschedulerapi.event

import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationUrlBuilder.transferUrl
import java.util.UUID

data class TransferMovementInformation(
  override val id: UUID,
  val movementId: UUID,
  override val source: DataSource,
) : SourceInformation,
  IdInformation

data class TransferMovementMigrated(
  override val additionalInformation: TransferMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.migrated"
    const val DESCRIPTION = "A transfer movement has been migrated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      movementId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementMigrated(
      TransferMovementInformation(id, movementId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementRecorded(
  override val additionalInformation: TransferMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.recorded"
    const val DESCRIPTION = "A transfer movement has been recorded"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      movementId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementRecorded(
      TransferMovementInformation(id, movementId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementDeleted(
  override val additionalInformation: TransferMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.deleted"
    const val DESCRIPTION = "A transfer movement has been deleted"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      movementId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementDeleted(
      TransferMovementInformation(id, movementId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementLogisticsChanged(
  override val additionalInformation: TransferMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.logistics-changed"
    const val DESCRIPTION = "The logistics of a transfer movement have changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      movementId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementLogisticsChanged(
      TransferMovementInformation(id, movementId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementRecategorised(
  override val additionalInformation: TransferMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.recategorised"
    const val DESCRIPTION = "The reason for a transfer movement has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      movementId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementRecategorised(
      TransferMovementInformation(id, movementId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementRelocated(
  override val additionalInformation: TransferMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.relocated"
    const val DESCRIPTION = "The destination of a transfer movement has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      movementId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementRelocated(
      TransferMovementInformation(id, movementId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovementOccurredAtChanged(
  override val additionalInformation: TransferMovementInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferMovementInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer-movement.occurred-at-changed"
    const val DESCRIPTION = "When a transfer movement occurred has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      movementId: UUID,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovementOccurredAtChanged(
      TransferMovementInformation(id, movementId, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
