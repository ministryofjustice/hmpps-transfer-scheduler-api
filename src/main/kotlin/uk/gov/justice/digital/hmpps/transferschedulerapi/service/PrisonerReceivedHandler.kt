package uk.gov.justice.digital.hmpps.transferschedulerapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PrisonerReceived
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.PrisonerReceivedInformation.Companion.BOOKING_SWITCHED_REASON
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.nomis.MigrationClient

@Service
class PrisonerReceivedHandler(private val migrationClient: MigrationClient) {
  fun handle(pre: PrisonerReceived) {
    if (pre.additionalInformation.reason != BOOKING_SWITCHED_REASON) return
    migrationClient.requestRepair(pre.additionalInformation.nomsNumber)
  }
}
