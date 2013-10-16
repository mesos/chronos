package com.airbnb.scheduler.api

import mesosphere.chaos.http.RestModule
import com.google.inject.Scopes

/**
 * @author Tobi Knaup
 */

class ChronosRestModule extends RestModule {

  override val guiceContainerUrl = "/scheduler/*"

  protected override def configureServlets() {
    super.configureServlets()

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
}
