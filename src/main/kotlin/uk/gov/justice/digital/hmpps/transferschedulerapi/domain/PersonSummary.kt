package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Predicate
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.FIRST_NAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.IDENTIFIER
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.LAST_NAME
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary.Companion.PRISON_CODE

@Entity
@Table(name = "person_summary")
final class PersonSummary(
  firstName: String,
  lastName: String,
  prisonCode: String?,
  cellLocation: String?,
  @Id
  @Size(max = 10)
  @Column(name = "person_identifier", nullable = false, length = 10)
  val identifier: String,
) {
  @Version
  @NotNull
  @Column(name = "version", nullable = false)
  var version: Int? = null
    private set

  @Size(max = 64)
  @NotNull
  @Column(name = "first_name", nullable = false, length = 64)
  var firstName: String = firstName
    private set

  @Size(max = 64)
  @NotNull
  @Column(name = "last_name", nullable = false, length = 64)
  var lastName: String = lastName
    private set

  @Size(max = 6)
  @Column(name = "prison_code", length = 6)
  var prisonCode: String? = prisonCode
    private set

  @Size(max = 64)
  @Column(name = "cell_location", length = 64)
  var cellLocation: String? = cellLocation
    private set

  fun update(firstName: String, lastName: String, prisonCode: String?, cellLocation: String?) = apply {
    if (this.firstName != firstName || this.lastName != lastName || this.prisonCode != prisonCode || this.cellLocation != cellLocation) {
      this.firstName = firstName
      this.lastName = lastName
      this.prisonCode = prisonCode
      this.cellLocation = cellLocation
    }
  }

  companion object {
    val IDENTIFIER: String = PersonSummary::identifier.name
    val FIRST_NAME: String = PersonSummary::firstName.name
    val LAST_NAME: String = PersonSummary::lastName.name
    val PRISON_CODE: String = PersonSummary::prisonCode.name
  }
}

interface PersonSummaryRepository : JpaRepository<PersonSummary, String>

fun <T> Join<T, PersonSummary>.matchesName(cb: CriteriaBuilder, name: String): Predicate {
  val matches = name.replace(",", " ").split("\\s".toRegex())
    .filter { it.isNotBlank() }
    .map {
      cb.or(
        cb.like(cb.lower(this[LAST_NAME]), "%${it.lowercase()}%", '\\'),
        cb.like(cb.lower(this[FIRST_NAME]), "%${it.lowercase()}%", '\\'),
      )
    }.toTypedArray()
  return cb.and(*matches)
}

fun <T> Join<T, PersonSummary>.matchesIdentifier(
  cb: CriteriaBuilder,
  identifier: String,
): Predicate = cb.equal(get<String>(IDENTIFIER), identifier.uppercase())

fun <T> Join<T, PersonSummary>.matchesPrisonCode(
  cb: CriteriaBuilder,
  prisonCode: String,
): Predicate = cb.equal(get<String>(PRISON_CODE), prisonCode)
