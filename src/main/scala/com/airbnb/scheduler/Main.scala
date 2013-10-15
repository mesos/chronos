package com.airbnb.scheduler

import java.util.logging.Logger
import java.util.concurrent.atomic.AtomicBoolean

import com.airbnb.dropwizard.assets.ConfiguredAssetsBundle
import com.airbnb.scheduler.api._
import com.airbnb.scheduler.config.{JobMetricsModule, ZookeeperModule, MainModule, SchedulerConfiguration}
import com.airbnb.scheduler.jobs.{MetricReporterService, JobScheduler}
import com.google.common.collect.ImmutableList
import com.google.inject.{AbstractModule, Injector, Guice}
import com.yammer.dropwizard.ScalaService
import com.yammer.dropwizard.config.{Bootstrap, Environment}
import com.yammer.dropwizard.bundles.ScalaBundle
import mesosphere.chaos.{AppConfiguration, App}
import mesosphere.chaos.http.{HttpService, HttpConf, HttpModule}
import mesosphere.chaos.metrics.MetricsModule
import mesosphere.marathon.MarathonModule
import mesosphere.marathon.api.MarathonRestModule
import mesosphere.marathon.event.EventModule
import mesosphere.marathon.event.http.HttpEventModule
import org.rogach.scallop.ScallopConf
import mesosphere.marathon.MarathonConfiguration
import mesosphere.marathon.event.EventConfiguration
import mesosphere.marathon.event.http.HttpEventConfiguration
import mesosphere.marathon.MarathonSchedulerService

/**
 * @author Tobi Knaup
 */
object Main extends App {

  val VERSION = "0.1.1-SNAPSHOT"

  val log = Logger.getLogger(getClass.getName)

  def modules() = {
    Seq(
      new HttpModule(conf),
      new MetricsModule,
      new MainModule(conf),
      new ZookeeperModule(conf),
      new JobMetricsModule(conf)
    )
  }

  //TODO(*): Rethink how this could be done.
  def getEventsModule(): Option[AbstractModule] = {
    if (conf.eventSubscriber.isSupplied) {
      conf.eventSubscriber() match {
        case "http_callback" => {
          log.warning("Using HttpCallbackEventSubscriber for event" +
            "notification")
          return Some(new HttpEventModule())
        }
        case _ => {
          log.warning("Event notification disabled.")
        }
      }
    }

    None
  }

  //TOOD(FL): Make Events optional / wire up.
  lazy val conf = new ScallopConf(args)
    with HttpConf with MarathonConfiguration with AppConfiguration
    with EventConfiguration with HttpEventConfiguration

  run(List(
    classOf[HttpService],
    classOf[MarathonSchedulerService]
  ))
}

/**
 * Main entry point to chronos using the Dropwizard framework.
 * @author Florian Leibert (flo@leibert.de)
 */
object Main extends ScalaService[SchedulerConfiguration] {
  private[this] val log = Logger.getLogger(getClass.getName)

  //TODO(FL): This is somewhat bad as the injector now carries state. Redo the guice/dw wiring.
  var injector: Injector = null
  val isLeader = new AtomicBoolean(false)

  def initialize(bootstrap: Bootstrap[SchedulerConfiguration]) {
    bootstrap.setName("chronos")
    bootstrap.addBundle(new ScalaBundle)
    bootstrap.addBundle(new ConfiguredAssetsBundle("/assets/build/", "/"))
  }

  def run(configuration: SchedulerConfiguration, environment: Environment) {
    log.info("---------------------")
    log.info("Initializing chronos.")
    log.info("---------------------")
    //All Modules need to be added to this list
    injector = Guice.createInjector(ImmutableList.of(
      new MainModule(configuration),
      new ZookeeperModule(configuration),
      new JobMetricsModule(configuration)
    ))
    val filter = injector.getInstance(classOf[RedirectFilter])

    environment.addFilter(filter, "/*")
    environment.addHealthCheck(injector.getInstance(classOf[SchedulerHealthCheck]))

    //Ensures the MesosJobFramework is managed by dropwizard, i.e. is started / stopped before HTTP service.
    environment.addResource(injector.getInstance(classOf[Iso8601JobResource]))
    environment.addResource(injector.getInstance(classOf[DependentJobResource]))
    environment.addResource(injector.getInstance(classOf[JobManagementResource]))
    environment.addResource(injector.getInstance(classOf[TaskManagementResource]))
    environment.addResource(injector.getInstance(classOf[GraphManagementResource]))
    environment.addResource(injector.getInstance(classOf[StatsResource]))

    environment.manage(injector.getInstance(classOf[JobScheduler]))
    environment.manage(injector.getInstance(classOf[MetricReporterService]))
  }
}
