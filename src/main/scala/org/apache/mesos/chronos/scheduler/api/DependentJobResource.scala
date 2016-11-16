package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs.core.Response
import javax.ws.rs.{POST, PUT, Path, Produces}

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._
import com.codahale.metrics.annotation.Timed
import com.google.common.base.Charsets
import com.google.inject.Inject

/**
 * The REST API for adding job-dependencies to the scheduler.
 * @author Florian Leibert (flo@leibert.de)
 */
@Path(PathConstants.dependentJobPath)
@Produces(Array("application/json"))
class DependentJobResource @Inject()(
                                      val jobScheduler: JobScheduler,
                                      val jobGraph: JobGraph) {

  private[this] val log = Logger.getLogger(getClass.getName)

  @POST
  @Timed
  def post(newJob: DependencyBasedJob): Response = {
    handleRequest(newJob)
  }

  def handleRequest(newJob: DependencyBasedJob): Response = {
    try {
      val oldJobOpt = jobGraph.lookupVertex(newJob.name)
      if (oldJobOpt.isEmpty) {
        log.info("Received request for job:" + newJob.toString)

        require(JobUtils.isValidJobName(newJob.name),
          "the job's name is invalid. Allowed names: '%s'".format(JobUtils.jobNamePattern.toString()))
        if (newJob.parents.isEmpty) throw new Exception("Error, parent does not exist")

        jobScheduler.loadJob(newJob)
        Response.noContent().build()
      } else {
        require(oldJobOpt.isDefined, "JobSchedule '%s' not found".format(newJob.name))

        val oldJob = oldJobOpt.get

        //TODO(FL): Ensure we're using job-ids rather than relying on jobs names for identification.
        assert(newJob.name == oldJob.name, "Renaming jobs is currently not supported!")

        require(newJob.parents.nonEmpty, "Error, parent does not exist")

        log.info("Received replace request for job:" + newJob.toString)
        require(jobGraph.lookupVertex(newJob.name).isDefined, "JobSchedule '%s' not found".format(newJob.name))
        //TODO(FL): Put all the logic for registering, deregistering and replacing dependency based jobs into one place.
        val parents = jobGraph.parentJobs(newJob)
        oldJob match {
          case j: DependencyBasedJob =>
            val newParentNames = parents.map(_.name)
            val oldParentNames = jobGraph.parentJobs(j).map(_.name)

            if (newParentNames != oldParentNames) {
              oldParentNames.foreach(jobGraph.removeDependency(_, oldJob.name))
              newParentNames.foreach(jobGraph.addDependency(_, newJob.name))
            }
          case j: ScheduleBasedJob =>
            log.info("Removing schedule for job: %s".format(j))
            parents.foreach(p => jobGraph.addDependency(p.name, newJob.name))
        }

        jobScheduler.updateJob(oldJob, newJob)

        log.info("JobSchedule parent: [ %s ], name: %s, command: %s".format(newJob.parents.mkString(","), newJob.name, newJob.command))
        log.info("Replaced job: '%s', oldJob: '%s', newJob: '%s'".format(
          newJob.name,
          new String(JobUtils.toBytes(oldJob), Charsets.UTF_8),
          new String(JobUtils.toBytes(newJob), Charsets.UTF_8)))

        Response.noContent().build()
      }
    } catch {
      case ex: IllegalArgumentException =>
        log.log(Level.INFO, "Bad Request", ex)
        Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage)
          .build()
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        Response.serverError().build()
    }
  }
}
