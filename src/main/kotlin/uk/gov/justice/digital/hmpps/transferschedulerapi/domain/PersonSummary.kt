package uk.gov.justice.digital.hmpps.transferschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository

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
