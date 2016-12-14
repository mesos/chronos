package org.apache.mesos.chronos.scheduler.api

import java.io.{InputStream, OutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.logging.{Level, Logger}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.google.inject.Inject
import mesosphere.chaos.http.HttpConf
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.jobs.JobScheduler

import scala.collection.JavaConverters._
import scala.language.postfixOps

/**
  * Simple filter that redirects to the leader if applicable.
  *
  * @author Florian Leibert (flo@leibert.de)
  */
class RedirectFilter @Inject()(
    val jobScheduler: JobScheduler,
    val config: SchedulerConfiguration with HttpConf)
    extends Filter {
  val log: Logger = Logger.getLogger(getClass.getName)

  def init(filterConfig: FilterConfig) {}

  def doFilter(rawRequest: ServletRequest,
               rawResponse: ServletResponse,
               chain: FilterChain) {
    rawRequest match {
      case request: HttpServletRequest =>
        val leaderData = jobScheduler.getLeader
        val response = rawResponse.asInstanceOf[HttpServletResponse]
        val currentId = "%s:%d".format(config.hostname(), config.httpPort())

        if (jobScheduler.isLeader || currentId == leaderData) {
          chain.doFilter(request, response)
        } else {
          var proxyStatus: Int = 200
          try {
            log.info("Proxying request to %s .".format(leaderData))

            val method = request.getMethod

            val proxy =
              buildUrl(leaderData, request)
                .openConnection()
                .asInstanceOf[HttpURLConnection]

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
              case "GET" | "HEAD" | "DELETE" =>
                proxy.setDoOutput(false)
              case _ =>
                proxy.setDoOutput(true)
                val proxyOutputStream = proxy.getOutputStream
                copy(request.getInputStream, proxyOutputStream)
                proxyOutputStream.close()
            }

            proxyStatus = proxy.getResponseCode
            response.setStatus(proxyStatus)

            val fields = proxy.getHeaderFields
            // getHeaderNames() and getHeaders() are known to return null, see:
            // http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getHeaders(java.lang.String)
            if (fields != null) {
              for ((name, values) <- fields.asScala) {
                if (name != null && values != null) {
                  for (value <- values.asScala) {
                    response.setHeader(name, value)
                  }
                }
              }
            }

            val responseOutputStream = response.getOutputStream
            copy(proxy.getInputStream, response.getOutputStream)
            proxy.getInputStream.close()
            responseOutputStream.close()
          } catch {
            case t: Exception =>
              if ((200 to 299) contains proxyStatus) {
                log.log(Level.WARNING, "Exception while proxying!", t)
                response.sendError(
                  500,
                  "Error proxying request to leader (maybe the leadership just changed?)\n\nError:\n" + ExceptionUtils
                    .getStackTrace(t))
              }
          }
        }
      case _ =>
    }
  }

  def copy(input: InputStream, output: OutputStream) = {
    val bytes = new Array[Byte](1024)
    Iterator
      .continually(input.read(bytes))
      .takeWhile(-1 !=)
      .foreach(read => output.write(bytes, 0, read))
  }

  def buildUrl(leaderData: String, request: HttpServletRequest) = {
    if (request.getQueryString != null) {
      new URL(
        "http://%s%s?%s"
          .format(leaderData, request.getRequestURI, request.getQueryString))
    } else {
      new URL("http://%s%s".format(leaderData, request.getRequestURI))
    }
  }

  def destroy() {
    //NO-OP
  }
}
