package uk.gov.justice.digital.hmpps.transferschedulerapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.SchedulerContext
import uk.gov.justice.digital.hmpps.transferschedulerapi.context.set
import uk.gov.justice.digital.hmpps.transferschedulerapi.domain.DataSource

@Configuration
class SchedulerContextConfiguration(private val contextInterceptor: SchedulerContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry
      .addInterceptor(contextInterceptor)
      .addPathPatterns("/**")
      .excludePathPatterns(
        "/queue-admin/retry-all-dlqs",
        "/health/**",
        "/info",
        "/ping",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
      )
  }
}

@Configuration
class SchedulerContextInterceptor : HandlerInterceptor {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    val contextSource = if (request.requestURI.startsWith("/sync") || request.requestURI.startsWith("/move/transfers")) {
      DataSource.NOMIS
    } else {
      DataSource.DPS
    }
    val caseloadId = request.getHeader(CaseloadIdHeader.NAME)
    SchedulerContext(getUsername(), source = contextSource, caseloadId = caseloadId).set()
    return true
  }

  override fun afterCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    ex: Exception?,
  ) {
    SchedulerContext.clear()
    super.afterCompletion(request, response, handler, ex)
  }

  private fun getUsername(): String = SecurityContextHolder
    .getContext()
    .authentication
    ?.name
    ?.trim()
    ?.takeUnless(String::isBlank)
    ?.also { if (it.length > 64) throw ValidationException("Username must be <= 64 characters") }
    ?: throw ValidationException("Could not find non empty username")
}
