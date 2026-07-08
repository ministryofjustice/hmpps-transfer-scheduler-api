package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.search

import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.PersonSummary

fun PersonSummary.nameFormats(): List<String> = listOf(
  "$firstName $lastName",
  "$lastName $firstName",
  "$lastName,$firstName",
  "$lastName, $firstName",
)
