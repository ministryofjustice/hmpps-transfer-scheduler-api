package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferReason
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.exception.NotFoundException
import java.time.LocalDate
import java.util.UUID

interface TransferRepository :
  JpaRepository<Transfer, UUID>,
  JpaSpecificationExecutor<Transfer>

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

fun startsOnOrAfter(start: LocalDate) = Specification<Transfer> { tr, _, cb ->
  val schedule = tr.join<Transfer, Schedule>(Transfer::schedule.name, JoinType.LEFT)
  cb.or(
    schedule.isNull,
    cb.greaterThanOrEqualTo(schedule.get(Schedule::start.name), start.atStartOfDay()),
  )
}

fun startsOnOrBefore(end: LocalDate) = Specification<Transfer> { tr, _, cb ->
  val schedule = tr.join<Transfer, Schedule>(Transfer::schedule.name, JoinType.LEFT)
  cb.or(
    schedule.isNull,
    cb.lessThan(schedule.get(Schedule::start.name), end.plusDays(1).atStartOfDay()),
  )
}

fun transferStatusCodeIn(codes: Set<TransferStatus.Code>) = Specification<Transfer> { tr, _, cb ->
  val status = tr.join<Transfer, TransferStatus>(Transfer::status.name, JoinType.INNER)
  status.get<TransferStatus.Code>(TransferStatus::code.name).`in`(codes)
}

fun transferReasonCodeIn(codes: Set<String>) = Specification<Transfer> { tr, _, cb ->
  val reason = tr.join<Transfer, TransferReason>(Transfer::reason.name, JoinType.INNER)
  reason.get<String>(TransferReason::code.name).`in`(codes)
}
