package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferPriority
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.clashes.ClashRange
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

interface TransferRepository :
  JpaRepository<Transfer, UUID>,
  JpaSpecificationExecutor<Transfer> {

  @EntityGraph("transfer.all")
  fun findByLegacyId(legacyId: Long): Transfer?

  @EntityGraph("transfer.all")
  override fun findById(id: UUID): Optional<Transfer>

  @EntityGraph("transfer.all")
  override fun findAllById(ids: Iterable<UUID>): List<Transfer>

  @EntityGraph("transfer.all")
  override fun findAll(spec: Specification<Transfer>, pageable: Pageable): Page<Transfer>

  fun countAllByPersonIdentifier(personIdentifier: String): Int

  @EntityGraph("transfer.all")
  fun findAllByPersonIdentifier(personIdentifier: String): List<Transfer>

  @Query(
    """
    select tr.id from Transfer tr
    where tr.legacyId in :legacyIds
    union
    select tr.id from Transfer tr
    where tr.person.identifier = :personIdentifier
    union 
    select tr.id from Transfer tr
    where tr.movement.id in :movementIds
    union
    select tr.id from Transfer tr
    where tr.movement.legacyId in :movementLegacyIds
  """,
  )
  fun findTransferIds(
    personIdentifier: String,
    legacyIds: Set<Long>,
    movementIds: Set<UUID>,
    movementLegacyIds: Set<String>,
  ): List<UUID>
}

fun TransferRepository.getTransfer(id: UUID): Transfer = findByIdOrNull(id) ?: throw NotFoundException("Transfer not found")

fun transferMatchesPrisonCode(prisonCode: String) = Specification<Transfer> { tr, _, cb ->
  cb.equal(tr.get<String>(Transfer::prisonCode.name), prisonCode)
}

fun transferPersonIdentifierIn(personIdentifiers: Set<String>) = Specification<Transfer> { tr, _, cb ->
  tr.get<Any>(Transfer::person.name).get<String>(IDENTIFIER).`in`(personIdentifiers)
}

fun startsBetween(start: LocalDate?, end: LocalDate?) = Specification<Transfer> { tr, _, cb ->
  if (start != null && end != null) {
    val schedule = tr.join<Transfer, Schedule>(Transfer::schedule.name, JoinType.INNER)
    cb.and(
      cb.greaterThanOrEqualTo(schedule.get(Schedule::start.name), start.atStartOfDay()),
      cb.lessThan(schedule.get(Schedule::start.name), end.plusDays(1).atStartOfDay()),
    )
  } else {
    val schedule = tr.join<Transfer, Schedule>(Transfer::schedule.name, JoinType.LEFT)
    cb.isNull(schedule)
  }
}

fun requestedOnBetween(start: LocalDate, end: LocalDate) = Specification<Transfer> { tr, _, cb ->
  val plan = tr.join<Transfer, Plan>(Transfer::plan.name, JoinType.INNER)
  cb.and(
    cb.greaterThanOrEqualTo(plan.get(Plan::requestedOn.name), start),
    cb.lessThanOrEqualTo(plan.get(Plan::requestedOn.name), end),
  )
}

fun transferStatusCodeIn(codes: Set<TransferStatus.Code>) = Specification<Transfer> { tr, _, _ ->
  val status = tr.join<Transfer, TransferStatus>(Transfer::status.name, JoinType.INNER)
  status.get<TransferStatus.Code>(TransferStatus::code.name).`in`(codes.map { it.name })
}

fun transferReasonCodeIn(codes: Set<String>) = Specification<Transfer> { tr, _, _ ->
  val reason = tr.join<Transfer, TransferReason>(Transfer::reason.name, JoinType.INNER)
  reason.get<String>(TransferReason::code.name).`in`(codes)
}

fun destinationCodeIn(codes: Set<String>) = Specification<Transfer> { tr, _, _ ->
  tr.get<String>(Transfer::destinationCode.name).`in`(codes)
}

fun logisticsCodeIn(codes: Set<String>) = Specification<Transfer> { tr, _, _ ->
  val logistics = tr.join<Transfer, TransferLogistics>(Transfer::logistics.name, JoinType.LEFT)
  logistics.get<String>(TransferReason::code.name).`in`(codes)
}

fun priorityCodeIn(codes: Set<TransferPriority.Code>) = Specification<Transfer> { tr, _, _ ->
  val plan = tr.join<Transfer, Plan>(Transfer::plan.name, JoinType.INNER)
  val priority = plan.join<Plan, TransferPriority>(Plan::priority.name, JoinType.INNER)
  priority.get<String>(TransferPriority::code.name).`in`(codes.map { it.value })
}

fun matchesStage(stage: TransferStage) = Specification<Transfer> { tr, _, cb ->
  cb.equal(tr.get<TransferStage>(Transfer::stage.name), stage)
}

fun clashesFor(personIdentifiers: Set<String>, ranges: Set<ClashRange>) = Specification<Transfer> { tr, _, cb ->
  val schedule = tr.join<Transfer, Schedule>(Transfer::schedule.name, JoinType.INNER)
  val rangeRestrictions = ranges.map {
    cb.and(
      cb.greaterThan(schedule.get(Schedule::start.name), it.start.toLocalDate()),
      cb.lessThan(schedule.get(Schedule::start.name), it.end.toLocalDate().plusDays(1)),
    )
  }
  cb.and(
    tr.get<Any>(Transfer::person.name).get<String>(IDENTIFIER).`in`(personIdentifiers),
    cb.or(rangeRestrictions),
  )
}
