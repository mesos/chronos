package com.airbnb.scheduler.api

import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.airbnb.scheduler.jobs.JobScheduler
import com.google.inject.Inject

/**
 * Simple filter that redirects to the leader if applicable.
 * @author Florian Leibert (flo@leibert.de)
 */
class RedirectFilter @Inject()(val jobScheduler: JobScheduler) extends Filter  {
  def init(filterConfig: FilterConfig) {}

  def doFilter(rawRequest: ServletRequest, rawResponse: ServletResponse, chain: FilterChain) {
    if (rawRequest.isInstanceOf[HttpServletRequest]) {
      val request = rawRequest.asInstanceOf[HttpServletRequest]
      if (jobScheduler.isLeader) {
        chain.doFilter(request, rawResponse)
      } else {
        val response = rawResponse.asInstanceOf[HttpServletResponse]

        val newUrl = "http://%s%s".format(jobScheduler.getLeader, request.getRequestURI)
        println(newUrl)
        response.setHeader("Location", newUrl )
        response.setStatus( HttpServletResponse.SC_TEMPORARY_REDIRECT)
      }
    }
  }

  def destroy() {
    //NO-OP
  }
}
