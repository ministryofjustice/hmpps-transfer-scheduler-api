package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.EntityManager
import org.hibernate.Interceptor
import org.hibernate.Transaction
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.ServiceConfig
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import java.util.UUID

@Component
class EntityInterceptor : Interceptor {
  private lateinit var em: EntityManager
  private lateinit var sc: ServiceConfig
  private val publishedEventKeys = ThreadLocal.withInitial { mutableSetOf<Pair<UUID, String>>() }

  @Autowired
  fun setDomainEventRepository(@Lazy entityManager: EntityManager) {
    em = entityManager
  }

  @Autowired
  fun setServiceConfig(serviceConfig: ServiceConfig) {
    sc = serviceConfig
  }

  override fun onFlushDirty(
    entity: Any,
    id: Any,
    currentState: Array<out Any>,
    previousState: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ): Boolean {
    if (entity is DomainEventProducer) {
      val migrating = SchedulerContext.get().migratingData
      entity.domainEvents().forEach {
        if (registerDomainEvent(it.entityId, it.event.eventType)) {
          em.persist(HmppsDomainEvent(it.event, it.entityId).apply { published = migrating || !it.publish })
        }
      }
    }
    return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types)
  }

  override fun onPersist(
    entity: Any,
    id: Any,
    state: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ): Boolean {
    if (entity is DomainEventProducer) {
      val migrating = SchedulerContext.get().migratingData
      entity.initialEvents().forEach {
        if (registerDomainEvent(it.entityId, it.event.eventType)) {
          em.persist(HmppsDomainEvent(it.event, it.entityId).apply { published = migrating || !it.publish })
        }
      }
    }
    return super.onPersist(entity, id, state, propertyNames, types)
  }

  private fun registerDomainEvent(entityId: UUID, eventType: String): Boolean = publishedEventKeys.get().none { it.first == entityId && it.second.endsWith(".migrated") } &&
    publishedEventKeys.get().add(entityId to eventType)

  override fun afterTransactionCompletion(tx: Transaction) {
    publishedEventKeys.get().clear()
    super.afterTransactionCompletion(tx)
  }

  override fun onRemove(
    entity: Any,
    id: Any,
    state: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ) {
    if (entity is DomainEventProducer) {
      val migrating = SchedulerContext.get().migratingData
      entity.deletionEvents().forEach {
        if (registerDomainEvent(it.entityId, it.event.eventType)) {
          em.persist(HmppsDomainEvent(it.event, it.entityId).apply { published = migrating || !it.publish })
        }
      }
    }
    super.onRemove(entity, id, state, propertyNames, types)
  }
}
