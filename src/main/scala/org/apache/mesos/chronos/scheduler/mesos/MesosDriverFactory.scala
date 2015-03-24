package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger

import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.Protos.{FrameworkInfo, Status}
import org.apache.mesos.{MesosSchedulerDriver, Scheduler}
import org.apache.mesos.Protos.Credential
import com.google.protobuf.ByteString
import java.io.{ FileInputStream, IOException }

/**
 * The chronos driver doesn't allow calling the start() method after stop() has been called, thus we need a factory to
 * create a new driver once we call stop() - which will be called if the leader abdicates or is no longer a leader.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosDriverFactory(val mesosScheduler: Scheduler, val frameworkInfo: FrameworkInfo, val config: SchedulerConfiguration) {

  private[this] val log = Logger.getLogger(getClass.getName)

  var mesosDriver: Option[MesosSchedulerDriver] = None

  def start() {
    val status = get().start()
    if (status != Status.DRIVER_RUNNING) {
      log.severe(s"MesosSchedulerDriver start resulted in status:$status. Committing suicide!")
      System.exit(1)
    }
  }

  def get(): MesosSchedulerDriver = {
    if (mesosDriver.isEmpty) {
      makeDriver()
    }
    mesosDriver.get
  }

  def makeDriver() {
    
    val driver = config.mesosAuthenticationPrincipal.get match {
      case Some(principal) =>
        val credential = buildMesosCredentials(principal, config.mesosAuthenticationSecretFile.get)
        new MesosSchedulerDriver(mesosScheduler, frameworkInfo, config.master(), credential)
      case None =>
        new MesosSchedulerDriver(mesosScheduler, frameworkInfo, config.master())
    }
    
    mesosDriver = Option(driver)
  }

  def close() {
    assert(mesosDriver.nonEmpty, "Attempted to close a non initialized driver")
    if (mesosDriver.isEmpty) {
      System.exit(1)
    }

    mesosDriver.get.stop(true)
    mesosDriver = None
  }


  /**
   * Create the optional credentials instance, used to authenticate calls from Chronos to Mesos.
   */
  def buildMesosCredentials(principal: String, secretFile: Option[String]): Credential = {

    val credentialBuilder = Credential.newBuilder()
      .setPrincipal(principal)

    secretFile foreach { file =>
      try {
        val secretBytes = ByteString.readFrom(new FileInputStream(file))
        credentialBuilder.setSecret(secretBytes)
      }
      catch {
        case cause: Throwable =>
          throw new IOException(s"Error reading authentication secret from file [$file]", cause)
      }
    }

    credentialBuilder.build()
  }

  
}
