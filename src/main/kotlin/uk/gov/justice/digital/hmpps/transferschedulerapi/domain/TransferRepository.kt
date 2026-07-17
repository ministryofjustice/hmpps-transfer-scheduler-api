package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferLogistics
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import java.time.LocalDate
import java.util.UUID

interface TransferRepository :
  JpaRepository<Transfer, UUID>,
  JpaSpecificationExecutor<Transfer> {
  fun findByLegacyId(legacyId: Long): Transfer?
}

fun TransferRepository.getTransfer(id: UUID): Transfer = findByIdOrNull(id) ?: throw NotFoundException("Transfer not found")

fun transferMatchesPrisonCode(prisonCode: String) = Specification<Transfer> { tr, _, cb ->
  cb.equal(tr.get<String>(Transfer::prisonCode.name), prisonCode)
}

fun transferMatchesPersonPrisonCode(prisonCode: String) = Specification<Transfer> { tr, _, cb ->
  tr.join<Transfer, PersonSummary>(Transfer::person.name, JoinType.INNER).matchesPrisonCode(cb, prisonCode)
}

fun transferMatchesPersonIdentifier(personIdentifier: String, prisonCode: String?) = Specification<Transfer> { tr, _, cb ->
  val person = tr.join<Transfer, PersonSummary>(Transfer::person.name, JoinType.INNER)
  cb.and(
    person.matchesIdentifier(cb, personIdentifier),
    prisonCode?.let { person.matchesPrisonCode(cb, it) } ?: cb.conjunction(),
  )
}

fun transferMatchesPersonName(name: String, prisonCode: String?) = Specification<Transfer> { tr, _, cb ->
  val person = tr.join<Transfer, PersonSummary>(Transfer::person.name, JoinType.INNER)
  cb.and(
    person.matchesName(cb, name),
    prisonCode?.let { person.matchesPrisonCode(cb, it) } ?: cb.conjunction(),
  )
}

fun startsBetween(start: LocalDate, end: LocalDate, stage: TransferStage?) = Specification<Transfer> { tr, _, cb ->
  if (stage == TransferStage.SCHEDULED) {
    val schedule = tr.join<Transfer, Schedule>(Transfer::schedule.name, JoinType.INNER)
    cb.and(
      cb.greaterThanOrEqualTo(schedule.get(Schedule::start.name), start.atStartOfDay()),
      cb.lessThan(schedule.get(Schedule::start.name), end.plusDays(1).atStartOfDay()),
    )
  } else {
    val schedule = tr.join<Transfer, Schedule>(Transfer::schedule.name, JoinType.LEFT)
    cb.or(
      cb.isNull(schedule),
      cb.and(
        cb.greaterThanOrEqualTo(schedule.get(Schedule::start.name), start.atStartOfDay()),
        cb.lessThan(schedule.get(Schedule::start.name), end.plusDays(1).atStartOfDay()),
      ),
    )
  }
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

fun matchesStage(stage: TransferStage) = Specification<Transfer> { tr, _, cb ->
  cb.equal(tr.get<TransferStage>(Transfer::stage.name), stage)
}
