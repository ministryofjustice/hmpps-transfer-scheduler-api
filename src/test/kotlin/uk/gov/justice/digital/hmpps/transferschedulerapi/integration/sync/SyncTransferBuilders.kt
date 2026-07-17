package uk.gov.justice.digital.hmpps.transferschedulerapi.integration.sync

import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferPriorityCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncMovement
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncSchedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncTransferRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncUser
import uk.gov.justice.digital.hmpps.transferschedulerapi.sync.SyncWaitlist
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

fun syncWaitList(
  requestDate: LocalDate = LocalDate.now(),
  waitListStatus: String = SyncWaitlist.PENDING,
  statusDate: LocalDate = LocalDate.now(),
  transferPriority: String = TransferPriorityCode.randomCode(),
  approved: Boolean = Random.nextBoolean(),
  approvedStaffId: Long? = null,
  outcomeReasonCode: String? = null,
  commentText1: String? = word(25),
) = SyncWaitlist(
  requestDate,
  waitListStatus,
  statusDate,
  transferPriority,
  approved,
  approvedStaffId,
  outcomeReasonCode,
  commentText1,
)

fun syncSchedule(
  start: LocalDateTime? = LocalDate.now().plusDays(7).atTime(10, 0),
  eventSubType: String = TransferReasonCode.randomCode(),
  eventStatus: String = SyncSchedule.SCHEDULED,
  commentText: String? = word(25),
  hiddenCommentText: String? = word(10),
  agyLocId: String = prisonCode(),
  toAgyLocId: String? = prisonCode(),
  outcomeReasonCode: String? = null,
  escortCode: String? = TransferLogisticsCode.randomCode(),
) = SyncSchedule(
  start,
  eventSubType,
  eventStatus,
  commentText,
  hiddenCommentText,
  agyLocId,
  toAgyLocId,
  outcomeReasonCode,
  escortCode,
)

fun syncMovement(
  offenderBookId: Long = newId(),
  movementSeq: Long = newId(),
  occurredAt: LocalDateTime = LocalDateTime.now(),
  movementReasonCode: String = TransferReasonCode.randomCode(),
  escortCode: String = TransferLogisticsCode.randomCode(),
  fromAgyLocId: String = prisonCode(),
  toAgyLocId: String = prisonCode(),
  active: Boolean = Random.nextBoolean(),
  comments: String? = word(30),
) = SyncMovement(
  offenderBookId,
  movementSeq,
  occurredAt,
  movementReasonCode,
  escortCode,
  fromAgyLocId,
  toAgyLocId,
  active,
  comments,
)

fun syncTransfer(
  dpsId: UUID? = null,
  eventId: Long = newId(),
  waitlist: SyncWaitlist? = null,
  schedule: SyncSchedule? = syncSchedule(),
  movement: SyncMovement? = null,
) = SyncTransfer(dpsId, eventId, waitlist, schedule, movement)

fun syncUser(username: String = username(), activeCaseloadId: String? = prisonCode()) = SyncUser(username, activeCaseloadId)

fun syncTransferRequest(
  syncTransfer: SyncTransfer = syncTransfer(),
  syncUser: SyncUser = syncUser(),
  occurredAt: LocalDateTime = LocalDateTime.now(),
) = SyncTransferRequest(
  transfer = syncTransfer,
  syncUser = syncUser,
  occurredAt = occurredAt,
)
