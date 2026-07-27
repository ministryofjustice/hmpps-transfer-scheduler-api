package uk.gov.justice.digital.hmpps.transferschedulerapi.event

import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.IntegrationUrlBuilder.transferUrl
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import java.util.UUID

data class TransferInformation(
  override val id: UUID,
  val stage: TransferStage,
  override val source: DataSource,
) : SourceInformation,
  IdInformation

data class TransferMigrated(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.migrated"
    const val DESCRIPTION = "A transfer has been migrated"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMigrated(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferRecorded(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.recorded"
    const val DESCRIPTION = "A transfer has been recorded"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferRecorded(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferDeleted(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.deleted"
    const val DESCRIPTION = "A transfer has been deleted"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferDeleted(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferPlanned(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.planned"
    const val DESCRIPTION = "A transfer has been planned"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferPlanned(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferScheduled(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.scheduled"
    const val DESCRIPTION = "A transfer has been scheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferScheduled(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferCancelled(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.cancelled"
    const val DESCRIPTION = "A transfer has been cancelled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferCancelled(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferExpired(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.expired"
    const val DESCRIPTION = "A transfer has expired"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferExpired(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferCompleted(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.completed"
    const val DESCRIPTION = "A transfer has been completed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferCompleted(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferRecategorised(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.recategorised"
    const val DESCRIPTION = "The reason for a transfer has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferRecategorised(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferRelocated(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.relocated"
    const val DESCRIPTION = "The destination of a transfer has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferRelocated(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferRescheduled(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.rescheduled"
    const val DESCRIPTION = "A transfer has been rescheduled"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferRescheduled(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferReprioritised(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.reprioritised"
    const val DESCRIPTION = "A transfer has been reprioritised"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferReprioritised(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferLogisticsChanged(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.logistics-changed"
    const val DESCRIPTION = "The logistics of a transfer have changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferLogisticsChanged(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferMovedToPlanning(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.moved-to-planning"
    const val DESCRIPTION = "A scheduled transfer has been moved to planning"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferMovedToPlanning(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class TransferInTransit(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.in-transit"
    const val DESCRIPTION = "A transfer is in transit"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = TransferInTransit(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class PlanRequestedOnChanged(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.planning-requested-changed"
    const val DESCRIPTION = "The date the plan of a transfer was requested has been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = PlanRequestedOnChanged(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class PlanCommentsChanged(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.planning-comments-changed"
    const val DESCRIPTION = "Comments for the plan of a transfer have been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = PlanCommentsChanged(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}

data class ScheduleCommentsChanged(
  override val additionalInformation: TransferInformation,
  override val personReference: PersonReference,
) : DomainEvent<TransferInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION
  override val detailUrl: String = transferUrl(id)

  companion object {
    const val EVENT_TYPE = "person.transfer.schedule-comments-changed"
    const val DESCRIPTION = "Comments for the schedule of a transfer have been changed"
    operator fun invoke(
      personIdentifier: String,
      id: UUID,
      stage: TransferStage,
      dataSource: DataSource = SchedulerContext.get().source,
    ) = ScheduleCommentsChanged(
      TransferInformation(id, stage, dataSource),
      PersonReference.withIdentifier(personIdentifier),
    )
  }
}
