package org.apache.mesos.chronos.scheduler.api

import java.io.{InputStream, OutputStream}
import java.net.URI
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

class WebJarServlet extends HttpServlet {
  private[this] val log = LoggerFactory.getLogger(getClass)
  private val BufferSize = 8192

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {

    def withResource[T](path: String)(fn: InputStream => T): Option[T] = {
      Option(getClass.getResourceAsStream(path)).flatMap { stream =>
        Try(stream.available()) match {
          case Success(_) => Some(fn(stream))
          case Failure(_) => None
        }
      }
    }

    def transfer(
                  in: InputStream,
                  out: OutputStream,
                  close: Boolean = true,
                  continue: => Boolean = true): Unit = {
      try {
        val buffer = new Array[Byte](BufferSize)
        @tailrec def read(): Unit = {
          val byteCount = in.read(buffer)
          if (byteCount >= 0 && continue) {
            out.write(buffer, 0, byteCount)
            out.flush()
            read()
          }
        }
        read()
      } finally { if (close) Try(in.close()) }
    }


    def sendResource(resourceURI: String, mime: String): Unit = {
      withResource(resourceURI) { stream =>
        resp.setContentType(mime)
        resp.setContentLength(stream.available())
        resp.setStatus(200)
        transfer(stream, resp.getOutputStream)
      } getOrElse {
        resp.sendError(404)
      }
    }

    def sendResourceNormalized(resourceURI: String, mime: String): Unit = {
      val normalized = new URI(resourceURI).normalize().getPath
      if (normalized.startsWith("/assets")) sendResource(normalized, mime)
      else resp.sendError(404, s"Path: $normalized")
    }

    //extract request data
    val jar = req.getServletPath // e.g. /ui
    var resource = req.getPathInfo // e.g. /fonts/icon.gif
    if (resource.endsWith("/")) resource = resource + "index.html" // welcome file
    val file = resource.split("/").last //e.g. icon.gif
    val mediaType = file.split("\\.").lastOption.getOrElse("") //e.g. gif
    val mime = Option(getServletContext.getMimeType(file)).getOrElse(mimeType(mediaType)) //e.g plain/text
    val resourceURI = s"/assets$jar$resource"

    //log request data, since the names are not very intuitive
    if (log.isDebugEnabled) {
      log.debug(
        s"""
           |pathinfo: ${req.getPathInfo}
           |context: ${req.getContextPath}
           |servlet: ${req.getServletPath}
           |path: ${req.getPathTranslated}
           |uri: ${req.getRequestURI}
           |jar: $jar
           |resource: $resource
           |file: $file
           |mime: $mime
           |resourceURI: $resourceURI
       """.stripMargin)
    }

    sendResourceNormalized(resourceURI, mime)
  }

  private[this] def mimeType(mediaType: String): String = {
    mediaType.toLowerCase match {
      case "eot" => "application/vnd.ms-fontobject"
      case "svg" => "image/svg+xml"
      case "ttf" => "application/font-ttf"
      case _ => "application/octet-stream"
    }
  }
}
