package org.apache.mesos.chronos.scheduler.api

import javax.inject.Named
import javax.validation.Validation

import org.apache.mesos.chronos.scheduler.jobs.BaseJob
import org.apache.mesos.chronos.utils.{JobDeserializer, JobSerializer}
import com.codahale.metrics.servlets.MetricsServlet
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.name.Names
import com.google.inject.servlet.ServletModule
import com.google.inject.{Provides, Scopes, Singleton}
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import mesosphere.chaos.http.{LogConfigServlet, PingServlet}
import mesosphere.chaos.validation.{ConstraintViolationExceptionMapper, JacksonMessageBodyProvider}

/**
 * @author Tobi Knaup
 */

class ChronosRestModule extends ServletModule {

  val guiceContainerUrl = "/v1/scheduler/*"

  // Override these in a subclass to mount resources at a different path
  val pingUrl = "/ping"
  val metricsUrl = "/metrics"
  val loggingUrl = "/logging"

  @Provides
  @Singleton
  def provideJacksonJsonProvider(
                                  @Named("restMapper") mapper: ObjectMapper): JacksonJsonProvider = {
    val mod = new SimpleModule("JobModule")
    mod.addSerializer(classOf[BaseJob], new JobSerializer)
    mod.addDeserializer(classOf[BaseJob], new JobDeserializer)
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(mod)
    new JacksonMessageBodyProvider(mapper,
      Validation.buildDefaultValidatorFactory().getValidator)
  }

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

    bind(classOf[WebJarServlet]).in(Scopes.SINGLETON)
    serve("/", "/assets/*").`with`(classOf[WebJarServlet])

    bind(classOf[Iso8601JobResource]).in(Scopes.SINGLETON)
    bind(classOf[DependentJobResource]).in(Scopes.SINGLETON)
    bind(classOf[JobManagementResource]).in(Scopes.SINGLETON)
    bind(classOf[TaskManagementResource]).in(Scopes.SINGLETON)
    bind(classOf[GraphManagementResource]).in(Scopes.SINGLETON)
    bind(classOf[StatsResource]).in(Scopes.SINGLETON)
    bind(classOf[RedirectFilter]).in(Scopes.SINGLETON)
    bind(classOf[CorsFilter]).in(Scopes.SINGLETON)
    //This filter will redirect to the master if running in HA mode.
    filter("/*").through(classOf[CorsFilter])
    filter("/*").through(classOf[RedirectFilter])
  }
}
