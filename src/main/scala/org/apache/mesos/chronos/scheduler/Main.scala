package org.apache.mesos.chronos.scheduler

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.{ Level, Logger }

import org.apache.mesos.chronos.scheduler.api._
import org.apache.mesos.chronos.scheduler.config._
import org.apache.mesos.chronos.scheduler.jobs.{JobScheduler, MetricReporterService, ZookeeperService}
import mesosphere.chaos.http.{HttpConf, HttpModule, HttpService}
import mesosphere.chaos.metrics.MetricsModule
import mesosphere.chaos.{App, AppConfiguration}
import org.rogach.scallop.ScallopConf


/**
 * Main entry point to chronos using the Chaos framework.
 * @author Florian Leibert (flo@leibert.de)
 */
object Main extends App {
  lazy val conf = new ScallopConf(args)
    with HttpConf with AppConfiguration with SchedulerConfiguration
    with GraphiteConfiguration with CassandraConfiguration
  val isLeader = new AtomicBoolean(false)
  private[this] val log = Logger.getLogger(getClass.getName)

  log.info("---------------------")
  log.info("Initializing chronos.")
  log.info("---------------------")

  def modules() = {
    Seq(
      new HttpModule(conf),
      new ChronosRestModule,
      new MetricsModule,
      new MainModule(conf),
      new ZookeeperModule(conf),
      new JobMetricsModule(conf),
      new JobStatsModule(conf)
    )
  }

  try {
    run(
      classOf[ZookeeperService],
      classOf[HttpService],
      classOf[JobScheduler],
      classOf[MetricReporterService]
    )
  } catch {
      case t: Throwable =>
        log.log(Level.SEVERE, s"Chronos has exited because of an unexpected error: ${t.getMessage}", t)
        System.exit(1)
  }
}
