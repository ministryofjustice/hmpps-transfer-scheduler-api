package uk.gov.justice.digital.hmpps.transferschedulerapi.service.history

import org.hibernate.envers.RevisionType
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.AuditRevision
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Identifiable

class AuditedEntity(
  val type: RevisionType,
  val revision: AuditRevision,
  val state: Identifiable,
)
