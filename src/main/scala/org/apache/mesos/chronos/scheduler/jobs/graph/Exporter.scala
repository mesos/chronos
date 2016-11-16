package org.apache.mesos.chronos.scheduler.jobs.graph

import java.io._

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.JobScheduler
import org.apache.mesos.chronos.scheduler.jobs.BaseJob
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats
import org.jgrapht.graph.DefaultEdge
import org.joda.time.DateTime

import scala.collection.mutable
import scala.collection.mutable.HashMap

/**
 * @author Florian Leibert (flo@leibert.de)
 */
object Exporter {

  def export(w: Writer, jobGraph: JobGraph, jobStats: JobStats) {
    val dag = jobGraph.dag
    val jobMap = new mutable.HashMap[String, BaseJob]
    import scala.collection.JavaConversions._
    dag.vertexSet.flatMap(jobGraph.lookupVertex).foreach(x => jobMap.put(x.name, x))
    jobMap.foreach({ case (k, v) => w.write("node,%s,%s,%s\n".format(k,getLastState(v).toString,jobStats.getJobState(k).toString))})
    for (e: DefaultEdge <- dag.edgeSet) {
      val source = dag.getEdgeSource(e)
      val target = dag.getEdgeTarget(e)
      w.write("link,%s,%s\n".format(source, target))
    }
  }

  def getLastState(job: BaseJob) = {
    if (job.lastSuccess.isEmpty && job.lastError.isEmpty) LastState.fresh
    else if (job.lastSuccess.isEmpty) LastState.failure
    else if (job.lastError.isEmpty) LastState.success
    else {
      val lastSuccessTime = DateTime.parse(job.lastSuccess)
      val lastErrorTime = DateTime.parse(job.lastError)
      if (lastSuccessTime.isAfter(lastErrorTime)) LastState.success
      else LastState.failure
    }
  }

  object LastState extends Enumeration {
    type LastState = Value
    val success, failure, fresh = Value
  }
}

