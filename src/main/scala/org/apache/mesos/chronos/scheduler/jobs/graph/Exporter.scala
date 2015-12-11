/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mesos.chronos.scheduler.jobs.graph

import java.io._

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.BaseJob
import org.apache.mesos.chronos.scheduler.jobs.stats.JobStats
import org.jgrapht.graph.DefaultEdge
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * @author Florian Leibert (flo@leibert.de)
 */
object Exporter {

  def export(w: Writer, jobGraph: JobGraph, jobStats: JobStats) {
    val dag = jobGraph.dag
    val jobMap = new mutable.HashMap[String, BaseJob]
    import scala.collection.JavaConversions._
    dag.vertexSet.flatMap(jobGraph.lookupVertex).foreach(x => jobMap.put(x.name, x))
    jobMap.foreach({ case (k, v) => w.write("node,%s,%s,%s\n".format(k, getLastState(v).toString, jobStats.getJobState(k).toString)) })
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

