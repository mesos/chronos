package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger

import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.Protos.{FrameworkInfo, Status}
import org.apache.mesos.{MesosSchedulerDriver, Scheduler}

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
    mesosDriver = Some(new MesosSchedulerDriver(mesosScheduler, frameworkInfo, config.master()))
  }

  def close() {
    assert(mesosDriver.nonEmpty, "Attempted to close a non initialized driver")
    if (mesosDriver.isEmpty) {
      System.exit(1)
    }

    mesosDriver.get.stop(true)
    mesosDriver = None
  }
}
