package org.apache.mesos.chronos

import mesosphere.chaos.http.HttpConf
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.rogach.scallop.ScallopConf

object ChronosTestHelper {
  def makeConfig(args: String*): SchedulerConfiguration with HttpConf = {
    val opts = new ScallopConf(args) with SchedulerConfiguration with HttpConf {
      // scallop will trigger sys exit
      override protected def onError(e: Throwable): Unit = throw e
    }
    opts.verify()
    opts
  }
}
