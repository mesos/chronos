package com.airbnb.scheduler.mesos

import com.airbnb.scheduler.config.SchedulerConfiguration
import org.apache.mesos.{Scheduler, MesosSchedulerDriver}
import org.apache.mesos.Protos.FrameworkInfo

/**
 * The mesos driver doesn't allow calling the start() method after stop() has been called, thus we need a factory to
 * create a new driver once we call stop() - which will be called if the leader abdicates or is no longer a leader.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosDriverFactory(val mesosScheduler: Scheduler, val frameworkInfo: FrameworkInfo, val config: SchedulerConfiguration) {

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
    get().start()
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
