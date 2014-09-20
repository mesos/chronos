package com.airbnb.scheduler

import java.util.logging.Logger
import java.util.concurrent.atomic.AtomicBoolean

import com.airbnb.scheduler.api._
import com.airbnb.scheduler.config._
import com.airbnb.scheduler.jobs.{ZookeeperService, MetricReporterService, JobScheduler}
import mesosphere.chaos.{AppConfiguration, App}
import mesosphere.chaos.http.{HttpService, HttpConf, HttpModule}
import mesosphere.chaos.metrics.MetricsModule
import org.rogach.scallop.ScallopConf


/**
 * Main entry point to chronos using the Chaos framework.
 * @author Florian Leibert (flo@leibert.de)
 */
object Main extends App {
  private[this] val log = Logger.getLogger(getClass.getName)

  val isLeader = new AtomicBoolean(false)

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

  log.info("---------------------")
  log.info("Initializing chronos.")
  log.info("---------------------")

  lazy val conf = new ScallopConf(args)
    with HttpConf with AppConfiguration with SchedulerConfiguration
    with GraphiteConfiguration with CassandraConfiguration

  run(
    classOf[ZookeeperService],
    classOf[HttpService],
    classOf[JobScheduler],
    classOf[MetricReporterService]
  )
}
