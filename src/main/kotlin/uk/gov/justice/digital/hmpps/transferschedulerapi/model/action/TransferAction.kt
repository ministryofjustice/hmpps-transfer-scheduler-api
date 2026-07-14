package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.Transfer

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface TransferAction : Action<Transfer>

data class TransferActions(@Valid val actions: List<TransferAction>, val reason: String?)
