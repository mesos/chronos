package org.apache.mesos.chronos.scheduler.jobs

import java.util.concurrent.{ConcurrentHashMap, Future, TimeUnit}
import java.util.logging.Logger

import com.codahale.metrics.{Gauge, MetricRegistry}
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.{ListenableFutureTask, ListeningScheduledExecutorService}
import com.google.inject.Inject
import org.apache.mesos.Protos.{TaskID, TaskState}
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.stats.{CurrentState, JobStats}
import org.apache.mesos.chronos.scheduler.mesos.{ MesosDriverFactory, MesosOfferReviver }
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

  val taskCache = CacheBuilder.newBuilder().maximumSize(5000L).build[String, TaskState]()

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
        //remove invalid task
        removeTask(taskId)
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

  def removeTask(taskId: String) {
    persistenceStore.removeTask(taskId)
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
      log.warning("Job '%s' no longer registered.".format(jobName))
    } else {
        val (_, _, attempt, _) = TaskUtils.parseTaskId(taskId)
        val job = jobOption.get
        jobsObserver.apply(JobQueued(job, taskId, attempt))
    }

    if (config.reviveOffersForNewJobs()) {
      mesosOfferReviver.reviveOffers()
    }
  }

  /**
   * Adds a task to the local queue, meaning it will be executed with the next resource offer. The task will be
   * dispatched to chronos as soon as both the task is in the front of the queue and a chronos offer comes in.
   * @param taskId task to run
   */
  def scheduleTask(taskId: String, job: BaseJob, persist: Boolean) {
    scheduleDelayedTask(new ScheduledTask(taskId, DateTime.now(DateTimeZone.UTC), job, this), 0, persist)
  }

  /**
   * Enqeueues a wrapped Task with a calculated delay, after this time, the job is added to the job queue. This means
   * that the job is not necessarily dispatched right away, as this depends both on the time an offer comes in as well
   * as the size of the job queue.
   * @param task the wrapped task
   * @param delay the delay in milliseconds
   */
  def scheduleDelayedTask(task: ScheduledTask, delay: Long, persist: Boolean) {
    log.info("Scheduling task '%s' with delay: '%d'".format(task.taskId, delay))
    if (persist) {
      persistTask(task.taskId, task.job)
    }
    val futureTask = ListenableFutureTask.create(task)
    val f = listeningExecutor.schedule(futureTask, delay, TimeUnit.MILLISECONDS)
    taskMapping.getOrElseUpdate(task.job.name, new mutable.ListBuffer()) += ((task.taskId, f))
  }

  def persistTask(taskId: String, baseJob: BaseJob) {
    persistenceStore.persistTask(taskId, JobUtils.toBytes(baseJob))
  }

  /**
   * Cancels all the taskMappings
   * @param baseJob BaseJob for which to cancel all tasks.
   */
  def cancelTasks(baseJob: BaseJob) {
    taskMapping.get(baseJob.name) match {
      case Some(i) =>
        i.foreach({ x =>
          log.info("Cancelling task: " + x._1)
          x._2.cancel(true)
        })
      case None => log.info("No tasks found that need to be cancelled")
    }
    taskMapping -= baseJob.name
    cancelMesosTasks(baseJob)
  }

  def cancelMesosTasks(job: BaseJob) {
    import scala.collection.JavaConversions._
    taskCache.asMap
      .filterKeys(TaskUtils.getJobNameForTaskId(_) == job.name)
      .filter(_._2 == TaskState.TASK_RUNNING)
      .foreach({ x =>
      log.warning("Killing task '%s'".format(x._1))
      mesosDriver.get().killTask(TaskID.newBuilder().setValue(x._1).build())
    })
  }

  /**
   * Removes all tasks from the persistence store that belong to a job.
   * @param baseJob BaseJob for which to remove all tasks from persistence store.
   */
  def removeTasks(baseJob: BaseJob) {
    log.info("Removing all tasks for job:" + baseJob)
    persistenceStore.getTaskIds(Some(baseJob.name)).foreach({ x =>
      persistenceStore.removeTask(x)
    })
  }
}
