package org.apache.mesos.chronos.scheduler.api

import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.{GET, Path, Produces}

import com.google.inject.Inject
import org.apache.mesos.chronos.scheduler.jobs.JobScheduler


@Path(PathConstants.getLeaderPattern)
@Produces(Array(MediaType.APPLICATION_JSON))
class LeaderResource @Inject()(
                                val jobScheduler: JobScheduler
                              ) {
  @GET
  def getLeader(): Response = {
    Response.ok(Map("leader" -> jobScheduler.getLeader)).build
  }
}
