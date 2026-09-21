package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.access.Roles
import uk.gov.justice.digital.hmpps.transferschedulerapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.publication
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.referencedata.TransferStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.ScheduleCommentsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferLogisticsChanged
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRecategorised
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRelocated
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferRescheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.event.TransferScheduled
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperations
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.schedule
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.config.TransferOperationsImpl.Companion.transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferLogisticsCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.referencedata.TransferReasonCode
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonRegisterMockServer.Companion.prison
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerRegisterExtension.Companion.prisonRegister
import uk.gov.justice.digital.hmpps.transferschedulerapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfersRequest
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.BulkTransfersResponse
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.Transfer
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.TransferStage
import uk.gov.justice.digital.hmpps.transferschedulerapi.verifyAgainst
import java.time.LocalDateTime
import java.util.UUID

class BulkTransferIntTest(
  @Autowired transferOps: TransferOperations,
) : IntegrationTestBase(),
  TransferOperations by transferOps {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(BULK_TRANSFER_URL)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    bulkTransfers(role = "ROLE_ANY__OTHER").expectStatus().isForbidden
  }

  @Test
  fun `200 ok - new transfers are created`() {
    val prison = prison()
    val destination = prison()
    val request = transfersRequest(destinationCode = destination.code)
    prisonerSearch.givenPrisoners(prison.code, request.transfers.map { it.personIdentifier }.toSet())
    prisonRegister.givenPrisons(setOf(prison, destination))

    val username = username()
    val res = bulkTransfers(request, username, prison.code).successResponse<BulkTransfersResponse>(HttpStatus.OK)

    val saved = request.transfers.map { tr ->
      val saved = requireNotNull(findTransfer(tr.id))
      assertThat(saved.status.code).isEqualTo(SCHEDULED.name)
      assertThat(saved.stage).isEqualTo(TransferStage.SCHEDULED)
      saved verifyAgainst tr
      res.transfers.first { it.id == tr.id } verifyAgainst saved

      verifyAudit(
        saved,
        RevisionType.ADD,
        setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!),
        SchedulerContext.get().copy(username = username, caseloadId = prison.code),
      )
      saved
    }

    verifyEventPublications(
      saved.first(),
      saved.map {
        TransferScheduled(it.person.identifier, it.id, it.stage).publication(it.id)
      }.toSet(),
    )
  }

  @Test
  fun `200 ok - existing transfers are updated`() {
    val prison = prison()
    val destination = prison()
    val existing = givenTransfer(transfer(prisonCode = prison.code, schedule = schedule(LocalDateTime.now().plusDays(1))))
    assertThat(existing.status.code).isEqualTo(SCHEDULED.name)
    val logisticsCode = generateSequence { TransferLogisticsCode.randomCode() }.first { it != existing.logistics?.code }
    val reasonCode = generateSequence { TransferReasonCode.randomCode() }.first { it != existing.reason.code }
    val request = BulkTransfersRequest(
      listOf(
        bulkRequest(destination.code, existing.person.identifier, logisticsCode, reasonCode, id = existing.id),
      ),
    )
    prisonRegister.givenPrisons(setOf(prison, destination))

    val username = username()
    val res = bulkTransfers(request, username, prison.code).successResponse<BulkTransfersResponse>(HttpStatus.OK)

    val saved = requireNotNull(findTransfer(request.transfers.single().id))
    assertThat(saved.status.code).isEqualTo(SCHEDULED.name)
    assertThat(saved.stage).isEqualTo(TransferStage.SCHEDULED)
    assertThat(saved.prisonCode).isEqualTo(prison.code)
    assertThat(saved.reason.code).isEqualTo(request.transfers.first().reasonCode)
    assertThat(saved.destinationCode).isEqualTo(destination.code)
    assertThat(saved.logistics?.code).isEqualTo(request.transfers.first().logisticsCode)
    assertThat(saved.schedule?.start).isEqualTo(request.transfers.first().start)
    assertThat(saved.schedule?.comments).isEqualTo(request.transfers.first().comments)
    res.transfers.single() verifyAgainst saved

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(HmppsDomainEvent::class.simpleName!!, Transfer::class.simpleName!!, Schedule::class.simpleName!!),
      SchedulerContext.get().copy(username = username, caseloadId = prison.code),
    )

    verifyEventPublications(
      saved,
      setOf(
        TransferRescheduled(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
        TransferRelocated(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
        TransferLogisticsChanged(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
        TransferRecategorised(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
        ScheduleCommentsChanged(saved.person.identifier, saved.id, saved.stage).publication(saved.id),
      ),
    )
  }

  private fun bulkTransfers(
    request: BulkTransfersRequest = transfersRequest(),
    username: String = DEFAULT_USERNAME,
    caseloadId: String? = null,
    role: String? = Roles.TRANSFER_SCHEDULER_UI,
  ) = webTestClient
    .put()
    .uri(BULK_TRANSFER_URL)
    .bodyValue(request)
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .headers { hc -> caseloadId?.also { hc.put(CaseloadIdHeader.NAME, listOf(it)) } }
    .exchange()

  companion object {
    const val BULK_TRANSFER_URL = "bulk/transfers"

    private fun bulkRequest(
      destinationCode: String = prisonCode(),
      personIdentifier: String = personIdentifier(),
      logisticsCode: String = TransferLogisticsCode.randomCode(),
      reasonCode: String = TransferReasonCode.randomCode(),
      comments: String? = word(20),
      start: LocalDateTime = LocalDateTime.now().plusDays(7),
      statusCode: TransferStatus.Code = SCHEDULED,
      id: UUID = newUuid(),
    ) = BulkTransfer(personIdentifier, statusCode, destinationCode, logisticsCode, reasonCode, start, comments, id)

    private fun transfersRequest(
      destinationCode: String = prisonCode(),
      numberOfTransfers: Int = 10,
    ) = BulkTransfersRequest((1..numberOfTransfers).map { bulkRequest(destinationCode) })
  }
}
