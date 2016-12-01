package org.apache.mesos.chronos.scheduler.jobs

import java.util.concurrent.{ConcurrentHashMap, Future, TimeUnit}
import java.util.logging.Logger

import com.codahale.metrics.{Gauge, MetricRegistry}
import com.google.common.cache.{Cache, CacheBuilder}
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.inject.Inject
import org.apache.mesos.Protos.{TaskID, TaskState}
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.mesos.{MesosDriverFactory, MesosOfferReviver}
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.convert.decorateAsScala._
import scala.collection.{mutable, _}

/**
 * Helps manage task state and the queue which is a buffer where tasks are held until offers come in via chronos.
 * @author Florian Leibert (flo@leibert.de)
 */
class TaskManager @Inject()(val listeningExecutor: ListeningScheduledExecutorService,
                            val persistenceStore: PersistenceStore,
                            val jobGraph: JobGraph,
                            val mesosDriver: MesosDriverFactory,
                            val jobsObserver: JobsObserver.Observer,
                            val registry: MetricRegistry,
                            val config: SchedulerConfiguration,
                            val mesosOfferReviver: MesosOfferReviver) {

  val log = Logger.getLogger(getClass.getName)

  /* index values into queues */
  val HIGH_PRIORITY = 0
  val NORMAL_PRIORITY = 1

  /* Maintain a queue for high priority and normal priority jobs. (Just like boarding for airlines...
   * we have no guarantees of forward progress. But jobs in the high priority line always go first.) */
  val queues = Array[java.util.concurrent.LinkedBlockingQueue[String]](
    new java.util.concurrent.LinkedBlockingQueue[String], // high priority
    new java.util.concurrent.LinkedBlockingQueue[String])
  // normal
  val names = Array[String]("High priority", "Normal priority")

  private val taskCache = CacheBuilder.newBuilder().maximumSize(5000L).build[String, TaskState]()
  def getTaskCache: Cache[String, TaskState] = {
    taskCache
  }

  val taskMapping: concurrent.Map[String, mutable.ListBuffer[(String, Future[_])]] =
    new ConcurrentHashMap[String, mutable.ListBuffer[(String, Future[_])]]().asScala

  val queueGauge = registry.register(
    MetricRegistry.name(classOf[TaskManager], "queueSize"),
    new Gauge[Long] {
      def getValue = queues(NORMAL_PRIORITY).size
    })

  val highQueueGauge = registry.register(
    MetricRegistry.name(classOf[TaskManager], "highQueueSize"),
    new Gauge[Long] {
      def getValue = queues(HIGH_PRIORITY).size
    })


  /**
   * Returns the first task in the job queue
   * @return a 2-tuple consisting of taskId (String) and job (BaseJob).
   */
  def getTask: Option[(String, BaseJob)] = {
    getTaskHelper(HIGH_PRIORITY).orElse(getTaskHelper(NORMAL_PRIORITY))
  }

  private def getTaskHelper(num: Integer) = {
    val queue = queues(num)
    val name = names(num)
    val taskId = queue.poll()
    if (taskId == null) {
      log.fine(s"$name queue empty")
      None
    } else {
      log.info(s"$name queue contains task: $taskId")
      val jobOption = jobGraph.getJobForName(TaskUtils.getJobNameForTaskId(taskId))

      //If the job was deleted after the taskId was added to the queue, the task could be empty.
      if (jobOption.isEmpty) {
        None
      } else if (jobOption.get.disabled) {
        jobsObserver.apply(JobExpired(jobOption.get, taskId))
        None
      } else {
        val jobArguments = TaskUtils.getJobArgumentsForTaskId(taskId)
        var job = jobOption.get

        if (jobArguments != null && !jobArguments.isEmpty) {
          job = JobUtils.getJobWithArguments(job, jobArguments)
        }

        Some(taskId, job)
      }
    }
  }

  /**
   * Returns the time that is left before the task needs to be handed off to chronos where it is immediately executed.
   * @param due DateTime when the job should be run
   * @return the number of milliseconds between current time and when the task is due
   */
  def getMillisUntilExecution(due: DateTime) = {
    scala.math.max(0L, due.getMillis - new DateTime(DateTimeZone.UTC).getMillis)
  }

  /**
   * Removes a future-task mapping thus signaling that a task has been added to the local queue awaiting execution from
   * chronos.
   * @param task ScheduledTask to remove
   */
  def removeTaskFutureMapping(task: ScheduledTask) {
    log.info("Removing task mapping")
    taskMapping.get(task.job.name) match {
      case Some(i) =>
        taskMapping += (task.job.name -> i.filter({
          x => x._1 != task.taskId
        }))
      case _ =>
    }
  }

  /**
   * Cancels all tasks that are delay scheduled with the underlying executor.
   */
  def flush() {
    taskMapping.clone().values.foreach (
      _.foreach {
        case (taskId, futureTask) =>
            log.info("Cancelling task '%s'".format(taskId))
            futureTask.cancel(true)
      }
    )
    taskMapping.clear()
    queues.foreach(_.clear())
  }

  def enqueue(taskId: String, highPriority: Boolean) {
    /* Don't want to change previous logging if we don't have to... */
    log.fine(s"Adding task '$taskId' to ${if (highPriority) "high priority" else ""} queue")
    val _priority = if (highPriority) HIGH_PRIORITY else NORMAL_PRIORITY
    this.synchronized {
      queues(_priority).add(taskId)
    }

    val jobName = TaskUtils.getJobNameForTaskId(taskId)
    val jobOption = jobGraph.lookupVertex(jobName)
     if (jobOption.isEmpty) {
      log.warning("JobSchedule '%s' no longer registered.".format(jobName))
    } else {
        val (_, _, attempt, _) = TaskUtils.parseTaskId(taskId)
        val job = jobOption.get
        jobsObserver.apply(JobQueued(job, taskId, attempt))
    }

    if (config.reviveOffersForNewJobs()) {
      mesosOfferReviver.reviveOffers()
    }
  }

  def cancelMesosTasks(job: BaseJob) {
    import scala.collection.JavaConversions._
    taskCache.asMap
      .filterKeys(TaskUtils.getJobNameForTaskId(_) == job.name)
      .filter(_._2 == TaskState.TASK_RUNNING)
      .foreach({ x =>
      log.warning("Killing task '%s'".format(x._1))
      mesosDriver.get.killTask(TaskID.newBuilder().setValue(x._1).build())
    })
  }

}
