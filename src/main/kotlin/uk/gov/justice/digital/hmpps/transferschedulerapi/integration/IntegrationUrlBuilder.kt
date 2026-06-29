package uk.gov.justice.digital.hmpps.transferschedulerapi.integration

import java.util.UUID

object IntegrationUrlBuilder {
  lateinit var baseUrl: String

  fun transferUrl(id: UUID): String = "$baseUrl/integrations/transfers/$id"
}
