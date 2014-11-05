package com.airbnb.scheduler.mesos

import com.airbnb.scheduler.config.SchedulerConfiguration
import org.apache.mesos.{Scheduler, MesosSchedulerDriver}
import org.apache.mesos.Protos.FrameworkInfo
import org.apache.mesos.Protos.Status
import java.util.logging.Logger

/**
 * The mesos driver doesn't allow calling the start() method after stop() has been called, thus we need a factory to
 * create a new driver once we call stop() - which will be called if the leader abdicates or is no longer a leader.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosDriverFactory(val mesosScheduler: Scheduler, val frameworkInfo: FrameworkInfo, val config: SchedulerConfiguration) {

  private[this] val log = Logger.getLogger(getClass.getName)

  var mesosDriver: Option[MesosSchedulerDriver] = None

  def makeDriver() {
    mesosDriver = Some(new MesosSchedulerDriver( mesosScheduler, frameworkInfo, config.master()))
  }

  def get(): MesosSchedulerDriver = {
    if (mesosDriver.isEmpty) {
      makeDriver()
    }
    mesosDriver.get
  }

  def start() {
    val status = get().start()
    if (status != Status.DRIVER_RUNNING) {
      log.severe("MesosSchedulerDriver start resulted in status:" + status + ". Committing suicide!")
      System.exit(1)
    }
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
