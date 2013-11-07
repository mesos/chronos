package com.airbnb.scheduler.api

import mesosphere.chaos.http.{LogConfigServlet, PingServlet, RestModule}
import com.google.inject.{Singleton, Provides, Scopes}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.airbnb.scheduler.jobs.BaseJob
import javax.inject.Named
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import mesosphere.chaos.validation.{ConstraintViolationExceptionMapper, JacksonMessageBodyProvider}
import javax.validation.Validation
import com.google.inject.name.Names
import com.codahale.metrics.servlets.MetricsServlet
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import com.google.inject.servlet.ServletModule

/**
 * @author Tobi Knaup
 */

class ChronosRestModule extends ServletModule {

  val guiceContainerUrl = "/scheduler/*"

  // Override these in a subclass to mount resources at a different path
  val pingUrl = "/ping"
  val metricsUrl = "/metrics"
  val loggingUrl = "/logging"

  protected override def configureServlets() {
    bind(classOf[ObjectMapper])
      .annotatedWith(Names.named("restMapper"))
      .toInstance(new ObjectMapper())

    bind(classOf[PingServlet]).in(Scopes.SINGLETON)
    bind(classOf[MetricsServlet]).in(Scopes.SINGLETON)
    bind(classOf[LogConfigServlet]).in(Scopes.SINGLETON)
    bind(classOf[ConstraintViolationExceptionMapper]).in(Scopes.SINGLETON)

    serve(pingUrl).`with`(classOf[PingServlet])
    serve(metricsUrl).`with`(classOf[MetricsServlet])
    serve(loggingUrl).`with`(classOf[LogConfigServlet])
    serve(guiceContainerUrl).`with`(classOf[GuiceContainer])

    bind(classOf[Iso8601JobResource]).in(Scopes.SINGLETON)
    bind(classOf[DependentJobResource]).in(Scopes.SINGLETON)
    bind(classOf[JobManagementResource]).in(Scopes.SINGLETON)
    bind(classOf[TaskManagementResource]).in(Scopes.SINGLETON)
    bind(classOf[GraphManagementResource]).in(Scopes.SINGLETON)
    bind(classOf[StatsResource]).in(Scopes.SINGLETON)
    bind(classOf[RedirectFilter]).in(Scopes.SINGLETON)
    //This filter will redirect to the master if running in HA mode.
    filter("/*").through(classOf[RedirectFilter])
  }

  @Provides
  @Singleton
  def provideJacksonJsonProvider(
      @Named("restMapper") mapper: ObjectMapper): JacksonJsonProvider = {
    val mod =  new SimpleModule("JobModule")
    mod.addSerializer(classOf[BaseJob], new JobSerializer)
    mod.addDeserializer(classOf[BaseJob], new JobsDeserializer)
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(mod)
    new JacksonMessageBodyProvider(mapper,
      Validation.buildDefaultValidatorFactory().getValidator)
  }
}
