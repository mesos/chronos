package org.apache.mesos.chronos.scheduler.mesos

import java.io.{ IOException, FileInputStream }
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{ Paths, Files }
import java.util.logging.Logger

import com.google.protobuf.ByteString
import mesosphere.chaos.http.HttpConf
import org.apache.mesos.Protos.{ Credential, FrameworkID, FrameworkInfo }
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.{ Protos, MesosSchedulerDriver, Scheduler, SchedulerDriver }

import scala.collection.JavaConverters.asScalaSetConverter

class SchedulerDriverBuilder {
  private[this] val log = Logger.getLogger(getClass.getName)

  def newDriver(config: SchedulerConfiguration with HttpConf,
                frameworkId: Option[FrameworkID],
                scheduler: Scheduler): SchedulerDriver = {
    def buildCredentials(principal: String, secretFile: String): Credential = {
      val credentialBuilder = Credential.newBuilder().setPrincipal(principal)

      try {
        val secretBytes = ByteString.readFrom(new FileInputStream(secretFile))

        val filePermissions = Files.getPosixFilePermissions(Paths.get(secretFile)).asScala
        if ((filePermissions & Set(PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE)).nonEmpty)
          log.warning(s"Secret file $secretFile should not be globally accessible.")

        credentialBuilder.setSecretBytes(secretBytes)
      }
      catch {
        case cause: Throwable =>
          throw new IOException(s"Error reading authentication secret from file [$secretFile]", cause)
      }

      credentialBuilder.build()
    }

    def buildFrameworkInfo(config: SchedulerConfiguration with HttpConf,
                           frameworkId: Option[FrameworkID]): Protos.FrameworkInfo = {
      val frameworkInfoBuilder = FrameworkInfo.newBuilder()
        .setName(config.mesosFrameworkName())
        .setCheckpoint(config.mesosCheckpoint())
        .setRole(config.mesosRole())
        .setFailoverTimeout(config.failoverTimeoutSeconds())
        .setUser(config.user())
        .setHostname(config.hostname())

      // Set the ID, if provided
      frameworkId.foreach(frameworkInfoBuilder.setId)

      if (config.webuiUrl.isSupplied) {
        frameworkInfoBuilder.setWebuiUrl(config.webuiUrl())
      }
      else if (config.sslKeystorePath.isDefined) {
        // ssl enabled, use https
        frameworkInfoBuilder.setWebuiUrl(s"https://${config.hostname()}:${config.httpsPort()}")
      }
      else {
        // ssl disabled, use http
        frameworkInfoBuilder.setWebuiUrl(s"http://${config.hostname()}:${config.httpPort()}")
      }

      // set the authentication principal, if provided
      config.mesosAuthenticationPrincipal.get.foreach(frameworkInfoBuilder.setPrincipal)

      frameworkInfoBuilder.build()
    }

    val frameworkInfo: FrameworkInfo = buildFrameworkInfo(config, frameworkId)

    val credential: Option[Credential] = config.mesosAuthenticationPrincipal.get.flatMap { principal =>
      config.mesosAuthenticationSecretFile.get.map { secretFile =>
        buildCredentials(principal, secretFile)
      }
    }

    credential match {
      case Some(cred) =>
        new MesosSchedulerDriver(scheduler, frameworkInfo, config.master(), cred)

      case None =>
        new MesosSchedulerDriver(scheduler, frameworkInfo, config.master())
    }
  }
}
