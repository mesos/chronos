package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class CorsFilter extends Filter {
  val log = Logger.getLogger(getClass.getName)

  def init(filterConfig: FilterConfig) {}
  def doFilter(rawRequest: ServletRequest, rawResponse: ServletResponse, chain: FilterChain) {
    rawResponse match {
      case response: HttpServletResponse =>
          log.debug("Adding cors header to api response")
          response.setHeader("Access-Control-Allow-Origin", "*");
          chain.doFilter(rawRequest, rawResponse)
      case _ =>
    }
  }
  def destroy() {}
}
