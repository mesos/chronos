package org.apache.mesos.chronos.scheduler.api

import java.util.logging.Logger
import javax.servlet._
import javax.servlet.http.HttpServletResponse

class CorsFilter extends Filter {
  private val log: Logger = Logger.getLogger(getClass.getName)

  def init(filterConfig: FilterConfig) {}

  def doFilter(rawRequest: ServletRequest, rawResponse: ServletResponse, chain: FilterChain) {
    rawResponse match {
      case response: HttpServletResponse =>
        log.fine("Adding cors header to api response")
        response.setHeader("Access-Control-Allow-Origin", "*")
        chain.doFilter(rawRequest, rawResponse)
      case _ =>
    }
  }

  def destroy() {}
}
