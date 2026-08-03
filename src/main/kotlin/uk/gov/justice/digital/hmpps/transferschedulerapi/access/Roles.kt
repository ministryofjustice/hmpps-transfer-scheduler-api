package uk.gov.justice.digital.hmpps.transferschedulerapi.access

object Roles {
  const val TRANSFER_SCHEDULER_UI = "ROLE_TRANSFERS__TRANSFER_SCHEDULER_UI"
  const val SCHEDULE_CLASHES_RO = "ROLE_SCHEDULES__CLASHES__RO"
  const val NOMIS_SYNC = "ROLE_TRANSFERS__SYNC__RW"

  fun allExcept(vararg except: String): List<String> = listOf(
    TRANSFER_SCHEDULER_UI,
    SCHEDULE_CLASHES_RO,
    NOMIS_SYNC,
  ).filter { it !in except }
}
