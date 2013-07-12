package com.airbnb.scheduler.state

import java.util.logging.{Level, Logger}
import scala.collection.mutable
import scala.Some

import com.airbnb.scheduler.config.SchedulerConfiguration
import com.airbnb.scheduler.jobs._
import com.google.inject.Inject
import com.twitter.common.zookeeper.{ZooKeeperUtils, ZooKeeperClient}
import org.apache.mesos.state.{InMemoryState, State}

/**
 * Handles storage and retrieval of job and task level data within the cluster.
 * @author Florian Leibert (flo@leibert.de)
 */

class MesosStatePersistenceStore @Inject()(val zk: ZooKeeperClient,
                                            val config: SchedulerConfiguration,
                                            val state: State = new InMemoryState)
  extends PersistenceStore {

  val log = Logger.getLogger(getClass.getName)
  val lock = new Object

  //TODO(FL): Redo string parsing once namespacing the states is implemented in mesos.
  //TODO(FL): Add task serialization such that in-flight tasks are accounted for.
  //TODO(FL): Add proper retry logic into the persistence.
  val jobPrefix = "J_"
  val taskPrefix = "T_"

  // There are many jobs in the system at any given point in time.
  val jobName = (name: String) => "%s%s".format(jobPrefix, name)

  // There are only few tasks (i.e. active tasks) in the system.
  val taskName = (name: String) => "%s%s".format(taskPrefix, name)

  /**
   * Retries a function
   * @param max the maximum retries
   * @param attempt the current attempt number
   * @param i the input
   * @param fnc the function to wrap
   * @tparam I the input parameter type
   * @tparam O the output parameter type
   * @return either Some(instanceOf[O]) or None if more exceptions occurred than permitted by max.
   */
  def retry[I, O](max: Int, attempt: Int, i: I, fnc: (I) => O): Option[O] = {
    try {
      Some(fnc(i))
    } catch {
      case t: Throwable => if (attempt < max) {
        log.log(Level.WARNING, "Retrying attempt:" + attempt, t)
        retry(max, attempt + 1, i, fnc)
      } else {
        log.severe("Giving up after attempts:" + attempt)
        None
      }
    }
  }

  def persistJob(job: BaseJob): Boolean = {
    log.info("Persisting job '%s' with data '%s'"format(job.name, job.toString))
    return (persistData(jobName(job.name), JobUtils.toBytes(job)))
  }

  //TODO(FL): Think about caching tasks locally such that we don't have to query zookeeper.
  def persistTask(name: String, data: Array[Byte]): Boolean = {
    log.finest("Persisting task: " + name)
    return (persistData(taskName(name), data))
  }

  def removeTask(taskId: String): Boolean = {
    log.fine("Removing task:" + taskId)
    remove(taskName(taskId))
  }

  def removeJob(job: BaseJob) {
    log.fine("Removing job:" + job.name)
    remove(jobName(job.name))
  }

  def getJob(name: String): BaseJob = {
    val bytes = state.fetch(jobName(name)).get
    JobUtils.fromBytes(bytes.value)
  }

  def getJobs: Iterator[BaseJob] = {

    import scala.collection.JavaConversions._

    state.names.get.filter(_.startsWith(jobPrefix))
      .map({
      x: String => JobUtils.fromBytes(state.fetch(x).get.value)
    })
  }

  def purgeTasks() {
    val tasks = getTaskIds(None)
    tasks.foreach({
      x =>
        log.warning("Removing task node in ZK:" + x)
        remove(x)
    })
  }

  def getTaskIds(filter: Option[String]): List[String] = {
    val results = new mutable.ListBuffer[String]

    import scala.collection.JavaConversions._
    for (f: String <- state.names.get) {
      if (f.startsWith(taskPrefix)) {
        if (filter.isEmpty || f.contains(filter.get)) {
          results += f.substring(taskPrefix.size)
        }
      }
    }
    results.toList
  }

  def getTasks: Map[String, Array[Byte]] = {
    lock.synchronized {
      val results = new mutable.HashMap[String, Array[Byte]]

      import scala.collection.JavaConversions._
      for (f: String <- state.names.get) {
        if (f.startsWith(taskPrefix)) {
          if (TaskUtils.isValidVersion(f)) {
            val data = state.fetch(f).get.value
            val taskId = f.substring(taskPrefix.size)
            results += (taskId -> data)
          } else {
             log.warning("Found old incompatible version of task, deleting:" + f)
            removeTask(f)
          }
        }
      }
      return results.toMap
    }
  }

  private def persistData(name: String, data: Array[Byte]): Boolean = {
    val existingVar = state.fetch(name).get

    if (existingVar.value.size == 0) {
      log.info("State %s does not exist yet. Adding to state".format(name))
    } else {
      log.info("Key for state exists already: %s".format(name))
    }

    val newVar = state.store(existingVar.mutate(data))

    val success = (newVar.get.value.deep == data.deep)

    log.info("State update successful: " + success)
    return success
  }

  private def remove(name: String): Boolean = {
    try {
      log.info("Purging entry '%s' via: %s".format(name, state.getClass.getName))
      val path = "%s/%s".format(config.zookeeperStateZnode, name)
      //Once state supports deletion, we can remove the ZK wiring.
      def fnc(s: String) {
        if (zk.get.exists(path, false)!=null) {
          zk.get.delete(path, ZooKeeperUtils.ANY_VERSION)
        }
      }
      retry[String, Unit](2, 0, path, fnc)
      (zk.get.exists(path, false) == null)
    } catch {
      case t: Throwable => {
        log.log(Level.WARNING, "Error while deleting zookeeper node: %s".format(name), t)
      }
      false
    }
  }

}
