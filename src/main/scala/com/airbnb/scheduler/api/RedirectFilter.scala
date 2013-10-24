package com.airbnb.scheduler.api

import scala.collection.JavaConverters._
import com.google.inject.Inject
import java.net.{HttpURLConnection, URL}
import java.io.{OutputStream, InputStream}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import javax.inject.Named
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

  val log = Logger.getLogger(getClass.getName)

  def copy(input: InputStream, output: OutputStream) = {
    val bytes = new Array[Byte](1024)
    Iterator
      .continually(input.read(bytes))
      .takeWhile(-1 !=)
      .foreach(read => output.write(bytes, 0, read))
  }

  def buildUrl(leaderData: String, request: HttpServletRequest) = {
    if (request.getQueryString != null) {
      new URL("http://%s%s?%s".format(leaderData, request.getRequestURI, request.getQueryString))
    } else {
      new URL("http://%s%s".format(leaderData, request.getRequestURI))
    }
  }

  def doFilter(rawRequest: ServletRequest,
               rawResponse: ServletResponse,
               chain: FilterChain) {
    try {
      if (rawRequest.isInstanceOf[HttpServletRequest]) {
        val request = rawRequest.asInstanceOf[HttpServletRequest]
        val leaderData = jobScheduler.getLeader
        val response = rawResponse.asInstanceOf[HttpServletResponse]

        if (jobScheduler.isLeader) {
          chain.doFilter(request, response)
        } else {
          log.info("Proxying request.")

          val method = request.getMethod

          val proxy =
            buildUrl(leaderData, request)
              .openConnection().asInstanceOf[HttpURLConnection]


          val names = request.getHeaderNames
          // getHeaderNames() and getHeaders() are known to return null, see:
          // http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getHeaders(java.lang.String)
          if (names != null) {
            for (name <- names.asScala) {
              val values = request.getHeaders(name)
              if (values != null) {
                proxy.setRequestProperty(name, values.asScala.mkString(","))
              }
            }
          }

          proxy.setRequestMethod(method)

          method match {
            case "GET" | "HEAD" =>
              proxy.setDoOutput(false)
            case _ =>
              proxy.setDoOutput(true)
              val proxyOutputStream = proxy.getOutputStream
              copy(request.getInputStream, proxyOutputStream)
              proxyOutputStream.close
          }

          response.setStatus(proxy.getResponseCode())

          val responseOutputStream = response.getOutputStream
          copy(proxy.getInputStream, response.getOutputStream)
          proxy.getInputStream.close
          responseOutputStream.close
        }
      }
    } catch {
      case t: Throwable => log.warning("Exception while proxying: " + t)
    }
  }

  def destroy() {
    //NO-OP
  }
}
