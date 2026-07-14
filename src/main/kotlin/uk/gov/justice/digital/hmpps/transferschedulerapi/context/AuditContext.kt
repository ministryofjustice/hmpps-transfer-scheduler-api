package uk.gov.justice.digital.hmpps.transferschedulerapi.context

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.AuditRevision
import uk.gov.justice.digital.hmpps.transferschedulerapi.service.TransferModifications

class AuditContext(var currentRevision: AuditRevision? = null) {
  companion object {
    fun get(): AuditContext? = TransferModifications.getAuditContext()
  }
}
