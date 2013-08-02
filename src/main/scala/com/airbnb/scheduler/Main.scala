package com.airbnb.scheduler

import java.util.logging.Logger
import java.util.concurrent.atomic.AtomicBoolean

import com.airbnb.dropwizard.assets.ConfiguredAssetsBundle
import com.airbnb.scheduler.api._
import com.airbnb.scheduler.config.{JobMetricsModule, ZookeeperModule, MainModule, SchedulerConfiguration}
import com.airbnb.scheduler.jobs.{JobScheduler}
import com.google.common.collect.ImmutableList
import com.google.inject.{Injector, Guice}
import com.yammer.dropwizard.ScalaService
import com.yammer.dropwizard.config.{Bootstrap, Environment}
import com.yammer.dropwizard.bundles.ScalaBundle

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

    environment.manage(injector.getInstance(classOf[JobScheduler]))
    environment.manage(injector.getInstance(classOf[JobMetricsModule]))
  }
}
