package org.apache.mesos.chronos.scheduler.graph

import java.io.StringWriter
import java.util.logging.Logger
import javax.annotation.concurrent.ThreadSafe

import org.apache.mesos.chronos.scheduler.jobs.{BaseJob, DependencyBasedJob}
import org.jgrapht.experimental.dag.DirectedAcyclicGraph
import org.jgrapht.ext.{DOTExporter, IntegerNameProvider, StringNameProvider}
import org.jgrapht.graph.DefaultEdge

import scala.collection.mutable.{HashMap, ListBuffer, Map, SynchronizedMap}

/**
 * This class provides methods to access dependency structures of jobs.
 * @author Florian Leibert (flo@leibert.de)
 */
@ThreadSafe
class JobGraph {
  val dag = new DirectedAcyclicGraph[String, DefaultEdge](classOf[DefaultEdge])
  val edgeInvocationCount = Map[DefaultEdge, Long]()
  private[this] val log = Logger.getLogger(getClass.getName)
  private[this] val jobNameMapping = new HashMap[String, BaseJob] with SynchronizedMap[String, BaseJob]
  private[this] val lock = new Object

  def parentJobs(job: DependencyBasedJob) = parentJobsOption(job) match {
    case None =>
      throw new IllegalArgumentException(s"requirement failed: Job ${job.name} does not have all parents defined!")
    case Some(jobs) =>
      jobs
  }

  def parentJobsOption(job: DependencyBasedJob): Option[List[BaseJob]] = {
    val vertexNamePairs = job.parents.map(x => (x, lookupVertex(x))).toList
    var failure = false
    val parents = vertexNamePairs.flatMap {
      case (x: String, y: Option[BaseJob]) =>
        y match {
          case None =>
            log.warning(s"Parent $x of job ${job.name} not found in job graph!")
            failure = true
            None
          case Some(baseJob: BaseJob) =>
            Some(baseJob)
        }
    }
    if (failure)
      None
    else
      Some(parents)
  }

  def getJobForName(name: String): Option[BaseJob] = {
    jobNameMapping.get(name)
  }

  def replaceVertex(oldVertex: BaseJob, newVertex: BaseJob) {
    require(oldVertex.name == newVertex.name, "Vertices need to have the same name!")
    lock.synchronized {
      jobNameMapping.put(oldVertex.name, newVertex)
    }
  }

  //TODO(FL): Documentation here and elsewhere in this file.
  def addVertex(vertex: BaseJob) {
    log.warning("Adding vertex:" + vertex.name)
    require(lookupVertex(vertex.name).isEmpty, "Vertex already exists in graph %s".format(vertex.name))
    require(!vertex.name.isEmpty, "In order to be added to the graph, the vertex must have a name")
    jobNameMapping.put(vertex.name, vertex)
    lock.synchronized {
      dag.addVertex(vertex.name)
    }
    log.warning("Current number of vertices:" + dag.vertexSet.size)
  }

  /* TODO(FL): Replace usage of this method with the hashmap */
  def lookupVertex(vertexName: String): Option[BaseJob] = {
    jobNameMapping.get(vertexName)
  }

  def removeVertex(vertex: BaseJob) {
    log.info("Removing vertex:" + vertex.name)
    require(!lookupVertex(vertex.name).isEmpty, "Vertex doesn't exist")
    jobNameMapping.remove(vertex.name)
    lock.synchronized {
      dag.removeVertex(vertex.name)
    }
    log.info("Current number of vertices:" + dag.vertexSet.size)
  }

  def addDependency(from: String, to: String) {
    lock.synchronized {
      if (!dag.vertexSet.contains(from) || !dag.vertexSet.contains(to))
        throw new NoSuchElementException("Vertex: %s not found in graph. Job rejected!".format(from))
      val edge = dag.addDagEdge(from, to)
      edgeInvocationCount.put(edge, 0L)
    }
  }

  def removeDependency(from: String, to: String) {
    lock.synchronized {
      if (!dag.vertexSet.contains(from) || !dag.vertexSet.contains(to))
        throw new NoSuchElementException("Vertex: %s not found in graph. Job rejected!".format(from))
      val edge = dag.removeEdge(from, to)
      edgeInvocationCount.remove(edge)
    }
  }

  def reset() {
    jobNameMapping.clear()
    lock.synchronized {
      edgeInvocationCount.clear()
      val names = ListBuffer[String]()
      import scala.collection.JavaConversions._
      dag.vertexSet.map({
        job =>
          names += job
      })
      dag.removeAllVertices(names)
    }
  }

  /**
   * Retrieves all the jobs that need to be triggered that depend on the finishedJob.
   * @param vertex
   * @return a list.
   */
  //TODO(FL): Avoid locking on every lookup.
  //TODO(FL): This method has some pretty serious side-effects. Refactor.
  def getExecutableChildren(vertex: String): List[String] = {
    val results = new scala.collection.mutable.ListBuffer[String]
    //TODO(FL): Make functional, making locking more efficient
    lock.synchronized {

      /*
        The algorithm:
        R = [ ]
        for each child in children
          E = edges(*, child)
          if |E| == 1
            R = R + child
          else
            edge_counts(vertex, child)++
            if edge_counts(vertex, child) == min(edge_counts(*, child))
             R = R + child
      */

      val children = getChildren(vertex)
      for (child <- children) {
        val edgesToChild = getEdgesToParents(child)
        if (edgesToChild.size == 1) {
          results += child
        }
        else {
          val currentEdge = dag.getEdge(vertex, child)
          if (!edgeInvocationCount.contains(currentEdge)) {
            edgeInvocationCount.put(currentEdge, 1L)
          } else {
            edgeInvocationCount.put(currentEdge, edgeInvocationCount.get(currentEdge).get + 1)
          }
          val count = edgeInvocationCount.get(currentEdge).get
          val min = edgesToChild.map(edgeInvocationCount.getOrElse(_, 0L)).min
          if (count == min)
            results += child
        }
      }
    }
    log.info("Dependents: [%s]".format(results.mkString(",")))
    results.toList
  }

  def getChildren(job: String): Iterable[String] = {
    import scala.collection.JavaConversions._
    lock.synchronized {
      dag.edgesOf(job)
        .filter(x => dag.getEdgeSource(x) == job)
        .map(x => dag.getEdgeTarget(x))
    }
  }

  def getEdgesToParents(child: String): Iterable[DefaultEdge] = {
    lock.synchronized {
      import scala.collection.JavaConversions._
      dag.edgesOf(child).filter(dag.getEdgeTarget(_).eq(child))
    }
  }

  def resetDependencyInvocations(vertex: String) {
    val edges = getEdgesToParents(vertex)
    lock.synchronized {
      edges.foreach({
        edge =>
          edgeInvocationCount.put(edge, 0)
      })
    }
  }

  def makeDotFile(): String = {
    val stw = new StringWriter
    val exporter = new DOTExporter[String, DefaultEdge](new IntegerNameProvider, new StringNameProvider, null)
    exporter.export(stw, dag)
    stw.flush()
    val result = stw.getBuffer.toString
    stw.close()
    result
  }
}
