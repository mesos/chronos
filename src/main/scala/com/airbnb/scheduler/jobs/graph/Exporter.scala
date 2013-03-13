package com.airbnb.scheduler.jobs.graph

import java.io._
import scala.collection.mutable.HashMap

import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.jobs.BaseJob
import org.jgrapht.graph.DefaultEdge
import org.joda.time.DateTime

/**
 * @author Florian Leibert (flo@leibert.de)
 */
object Exporter {

  object LastState extends Enumeration {
    type LastState = Value
    val success, failure, fresh = Value
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

  def export(w: Writer, jobGraph: JobGraph) {
    val dag = jobGraph.dag
    val jobMap = new HashMap[String, BaseJob]
    import scala.collection.JavaConversions._
    dag.vertexSet.flatMap(jobGraph.lookupVertex).foreach(x => jobMap.put(x.name, x))
    jobMap.foreach({ case (k, v) => w.write("node,%s,%s\n".format(k,getLastState(v).toString)) })
    for (e: DefaultEdge <- dag.edgeSet) {
      val source = dag.getEdgeSource(e)
      val target = dag.getEdgeTarget(e)
      w.write("link,%s,%s\n".format(source, target))
    }
  }
}

