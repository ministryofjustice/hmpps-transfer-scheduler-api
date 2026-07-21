package uk.gov.justice.digital.hmpps.transferschedulerapi.service.history

import jakarta.persistence.EntityManager
import org.hibernate.envers.AuditReader
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.hibernate.envers.query.AuditEntity.revisionNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.AuditRevision
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Movement
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.manageusers.UserDetails
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.AuditedAction
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

@Transactional(readOnly = true)
@Service
class TransferHistoryService(
  private val entityManager: EntityManager,
  private val managerUsers: ManageUsersClient,
) {

  fun changes(id: UUID): AuditHistory {
    val audited = getAuditedChanges(id, null)
    if (audited.isEmpty()) {
      throw NotFoundException("History not found")
    }
    return AuditHistory(audited.history(id))
  }

  fun changesForRevision(id: UUID, revisionId: Long): AuditedAction? {
    val audited = getAuditedChanges(id, revisionId)
    return when (audited.size) {
      0 -> throw NotFoundException("History not found")
      1 -> null
      else -> audited.history(id).lastOrNull()
    }
  }

  fun getStatusChanges(id: UUID): List<StatusChanged> {
    val auditReader = AuditReaderFactory.get(entityManager)
    val transferChanges = auditReader.getRevisions(Transfer::class, id, null)
    val audit = transferChanges.sortedBy { it.revision.timestamp }
    return audit.mapIndexedNotNull { idx, rev ->
      if (idx == 0) return@mapIndexedNotNull rev.statusChange(null)
      val prev = (audit[idx - 1].state as Transfer)
      val next = (rev.state as Transfer)
      if (prev.status.code != next.status.code) {
        rev.statusChange(TransferStatus.Code.valueOf(prev.status.code))
      } else {
        null
      }
    }
  }

  private fun AuditedEntity.statusChange(prev: TransferStatus.Code?): StatusChanged? = if (state is Transfer) {
    StatusChanged(revision.username!!, revision.timestamp!!, prev, TransferStatus.Code.valueOf(state.status.code))
  } else {
    null
  }

  private fun Map<AuditRevision, List<AuditedAction.Change>>.history(id: UUID): List<AuditedAction> {
    val revisions = keys.associateBy { it.id!! }
    val users = managerUsers.getUsersDetails(revisions.values.mapNotNull { it.username }.toSet())
      .associateBy { it.username }
    val domainEvents = getDomainEvents(id, revisions.keys)
    val changes = map { it.key.id to it.value }.toMap()
    return revisions.keys.toList().sorted().actions(
      { requireNotNull(revisions[it]) },
      { requireNotNull(users[it]) },
      { domainEvents[it] ?: emptyList() },
      { changes[it] ?: emptyList() },
    )
  }

  private fun getAuditedChanges(id: UUID, revisionId: Long?): Map<AuditRevision, List<AuditedAction.Change>> {
    val auditReader = AuditReaderFactory.get(entityManager)
    val transferChanges = auditReader.getRevisions(Transfer::class, id, revisionId).changes()
    val planChanges = auditReader.getRevisions(Plan::class, id, revisionId).changes()
    val scheduleChanges = auditReader.getRevisions(Schedule::class, id, revisionId).changes()
    val movementChanges = auditReader.getRevisions(Movement::class, id, revisionId).changes()
    val all: List<Pair<AuditRevision, List<AuditedAction.Change>>> =
      (transferChanges + planChanges + scheduleChanges + movementChanges)
    val revisions = all.associate { it.first.id!! to it.first }
    val revisionChanges = all.flatMap { (k, v) -> v.map { k.id!! to it } }.groupBy({ it.first }, { it.second })
    return revisions.keys.associate { revisions[it]!! to (revisionChanges[it] ?: emptyList()) }
  }

  private fun AuditReader.getRevisions(clazz: KClass<*>, id: UUID, revisionId: Long?): List<AuditedEntity> {
    val history = createQuery()
      .forRevisionsOfEntity(clazz.java, false, false)
      .add(AuditEntity.id().eq(id))
      .resultList.filterIsInstance<Array<*>>()
      .map { it.asAuditedEntity() }
      .sortedByDescending { it.revision.id }
    return (revisionId?.let { rev -> history.dropWhile { it.revision.id!! > rev }.take(2) } ?: history).reversed()
  }

  private fun Array<*>.asAuditedEntity(): AuditedEntity {
    val revision = this[1] as AuditRevision
    val type = this[2] as RevisionType
    return AuditedEntity(type, revision, this[0] as Identifiable)
  }

  private fun getDomainEvents(id: UUID, revisionIds: Set<Long>): Map<Long, List<String>> {
    val auditReader = AuditReaderFactory.get(entityManager)
    return auditReader
      .createQuery()
      .forRevisionsOfEntity(HmppsDomainEvent::class.java, false, false)
      .add(revisionNumber().`in`(revisionIds))
      .add(AuditEntity.property(HmppsDomainEvent::entityId.name).eq(id))
      .addProjection(revisionNumber())
      .addProjection(AuditEntity.property(HmppsDomainEvent::eventType.name))
      .resultList.filterIsInstance<Array<*>>()
      .map { it[0] as Long to it[1] as String }.groupBy({ it.first }, { it.second })
  }

  private fun List<AuditedEntity>.changes(): List<Pair<AuditRevision, List<AuditedAction.Change>>> = mapIndexed { idx, audited ->
    if (idx == 0) {
      audited.revision to listOf()
    } else {
      audited.revision to audited.state.changesFrom(this[idx - 1].state)
    }
  }

  private fun List<Long>.actions(
    revision: (Long) -> AuditRevision,
    user: (String) -> UserDetails,
    events: (Long) -> List<String>,
    changes: (Long) -> List<AuditedAction.Change>,
  ): List<AuditedAction> = mapIndexed { idx, revisionId ->
    val revision = revision(revisionId)
    val user = user(revision.username!!)
    val de = events(revisionId)
    if (idx == 0) {
      AuditedAction(
        AuditedAction.User(user.username, user.name),
        revision.timestamp!!,
        de,
        revision.reason,
        listOf(),
      )
    } else {
      AuditedAction(
        AuditedAction.User(user.username, user.name),
        revision.timestamp!!,
        de,
        revision.reason,
        changes(revisionId),
      )
    }
  }

  private fun Any?.asChangeValue(): Any? = when (this) {
    is ReferenceData -> description
    is Collection<*> -> map { it.asChangeValue() }
    else -> this
  }

  private fun Identifiable.changesFrom(previous: Identifiable): List<AuditedAction.Change> = when (this) {
    is Transfer if previous is Transfer -> Transfer.auditedProperties().changesBetween(previous, this)
    is Plan if previous is Plan -> Plan.auditedProperties().changesBetween(previous, this)
    is Schedule if previous is Schedule -> Schedule.auditedProperties().changesBetween(previous, this)
    is Movement if previous is Movement -> Movement.auditedProperties().changesBetween(previous, this)
    else -> return emptyList()
  }

  private fun <T : Identifiable> List<KMutableProperty1<T, out Any?>>.changesBetween(previous: T, new: T) = mapNotNull {
    val change = it(new).asChangeValue()
    val previous = it(previous).asChangeValue()
    if (change != previous) {
      AuditedAction.Change(it.name, previous, change)
    } else {
      null
    }
  }
}
