package uk.gov.justice.digital.hmpps.transferschedulerapi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.PlanRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.ScheduleRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferRequest
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Plan as PlanEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule as ScheduleEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer as TransferEntity
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Plan as PlanModel
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Schedule as ScheduleModel
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer as TransferModel

infix fun TransferModel.verifyAgainst(
  transfer: TransferEntity,
) {
  assertThat(id).isEqualTo(transfer.id)
  assertThat(person.identifier).isEqualTo(transfer.person.identifier)
  assertThat(status.code).isEqualTo(transfer.status.code)
  assertThat(reason.code).isEqualTo(transfer.reason.code)
  assertThat(prison.code).isEqualTo(transfer.prisonCode)
  assertThat(destination?.code).isEqualTo(transfer.destinationCode)
  assertThat(logistics?.code).isEqualTo(transfer.logistics?.code)
  assertThat(stage).isEqualTo(transfer.stage)
  check(nullStateIsEqual(plan, transfer.plan)) { "Invalid plan state" }
  check(nullStateIsEqual(schedule, transfer.schedule)) { "Invalid plan state" }
  plan?.also { it verifyAgainst transfer.plan!! }
  schedule?.also { it verifyAgainst transfer.schedule!! }
}

infix fun PlanModel.verifyAgainst(plan: PlanEntity) {
  assertThat(requestedOn).isEqualTo(plan.requestedOn)
  assertThat(priority.code).isEqualTo(plan.priority.code)
  assertThat(comments).isEqualTo(plan.comments)
}

infix fun ScheduleModel.verifyAgainst(schedule: ScheduleEntity) {
  assertThat(start.truncatedTo(ChronoUnit.SECONDS))
    .isCloseTo(schedule.start.truncatedTo(ChronoUnit.SECONDS), within(1, ChronoUnit.SECONDS))
  assertThat(comments).isEqualTo(schedule.comments)
}

infix fun TransferEntity.verifyAgainst(request: TransferRequest) {
  assertThat(reason.code).isEqualTo(request.reasonCode)
  assertThat(destinationCode).isEqualTo(request.destinationCode)
  assertThat(logistics?.code).isEqualTo(request.logisticsCode)
  check(nullStateIsEqual(plan, request.plan)) { "Invalid plan state" }
  check(nullStateIsEqual(schedule, request.schedule)) { "Invalid plan state" }
  plan?.also { it verifyAgainst request.plan!! }
  schedule?.also { it verifyAgainst request.schedule!! }
}

infix fun PlanEntity.verifyAgainst(request: PlanRequest) {
  assertThat(requestedOn).isEqualTo(request.requestedOn)
  assertThat(priority.code).isEqualTo(request.priorityCode)
  assertThat(comments).isEqualTo(request.comments)
}

infix fun ScheduleEntity.verifyAgainst(request: ScheduleRequest) {
  assertThat(start.truncatedTo(ChronoUnit.SECONDS))
    .isCloseTo(request.start.truncatedTo(ChronoUnit.SECONDS), within(1, ChronoUnit.SECONDS))
  assertThat(comments).isEqualTo(request.comments)
}

private fun nullStateIsEqual(first: Any?, second: Any?): Boolean = (first == null && second == null) || (first != null && second != null)
