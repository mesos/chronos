package com.airbnb.scheduler.jobs

import java.util.concurrent.{Future, TimeUnit}
import java.util.logging.Logger
import collection.mutable

import com.airbnb.scheduler.graph.JobGraph
import com.airbnb.scheduler.state.PersistenceStore
import com.airbnb.scheduler.mesos.MesosDriverFactory
import com.google.common.util.concurrent.{ListenableFutureTask, ListeningScheduledExecutorService}
import com.google.inject.Inject
import com.google.common.cache.CacheBuilder
import com.codahale.metrics.{MetricRegistry, Gauge}
import org.apache.mesos.Protos.{TaskID, TaskState}
import org.joda.time.{DateTimeZone, DateTime}

/**
 * Helps manage task state and the queue which is a buffer where tasks are held until offers come in via mesos.
 * @author Florian Leibert (flo@leibert.de)
 */
class TaskManager @Inject()(val listeningExecutor: ListeningScheduledExecutorService,
                             val persistenceStore: PersistenceStore,
                             val jobGraph: JobGraph,
                             val mesosDriver: MesosDriverFactory,
                             val registry: MetricRegistry) {

  val log = Logger.getLogger(getClass.getName)

  /* index values into queues */
  val HIGH_P = 0
  val NORMAL_P = 1
  val LOW_P = 2

  // These queues contains the task_ids  (high, medium, low) priorities
  val queues = Array[java.util.concurrent.LinkedBlockingQueue[String]](
    new java.util.concurrent.LinkedBlockingQueue[String], // high priority
    new java.util.concurrent.LinkedBlockingQueue[String], // normal
    new java.util.concurrent.LinkedBlockingQueue[String]) // low
  val names = Array[String]("High priority", "Normal priority", "Low priority")

  val taskCache = CacheBuilder.newBuilder().maximumSize(5000L).build[String, TaskState]()

  val taskMapping = new mutable.HashMap[String, mutable.ListBuffer[(String, Future[_])]] with
    collection.mutable.SynchronizedMap[String, mutable.ListBuffer[(String, Future[_])]]

  val queueGauge = registry.register(
    MetricRegistry.name(classOf[TaskManager], "queueSize"),
    new Gauge[Long] {
      def getValue() = queues(NORMAL_P).size
    })

  val lowQueueGauge = registry.register(
    MetricRegistry.name(classOf[TaskManager], "lowQueueSize"),
    new Gauge[Long] {
      def getValue() = queues(LOW_P).size
    })

  val highQueueGauge = registry.register(
    MetricRegistry.name(classOf[TaskManager], "highQueueSize"),
    new Gauge[Long] {
      def getValue() = queues(HIGH_P).size
    })


  /**
   * Returns the first task in the job queue
   * @return a 2-tuple consisting of taskId (String) and job (BaseJob).
   */
  def getTask: Option[(String, BaseJob)] = {
    getTaskHelper(HIGH_P).orElse(getTaskHelper(NORMAL_P)).orElse(getTaskHelper(LOW_P))
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
      } else {
        Some(taskId, jobOption.get)
      }
    }
  }

  /**
   * Returns the time that is left before the task needs to be handed off to mesos where it is immediately executed.
   * @param due
   * @return the number of milliseconds between current time and when the task is due
   */
  def getMillisUntilExecution(due: DateTime) = {
    scala.math.max(0L, due.getMillis - new DateTime(DateTimeZone.UTC).getMillis)
  }

  /**
   * Removes a future-task mapping thus signaling that a task has been added to the local queue awaiting execution from
   * mesos.
   * @param task
   */
  def removeTaskFutureMapping(task: ScheduledTask) {
    log.info("Removing task mapping")
    taskMapping.get(task.job.name) match {
      case Some(i) => {
        taskMapping += (task.job.name -> i.filter({
          x => x._1 != task.taskId
        }))
      }
      case _ =>
    }
  }

  /**
   * Cancels all tasks that are delay scheduled with the underlying executor.
   */
  def flush() {
    taskMapping.clone().values.map({
      f =>
        f.foreach({
          (f) =>
            log.info("Cancelling task '%s'".format(f._1))
            f._2.cancel(true)
        })
    })
    taskMapping.clear()
    queues.foreach(_.clear())
  }

  def persistTask(taskId: String, baseJob: BaseJob) {
    persistenceStore.persistTask(taskId, JobUtils.toBytes(baseJob))
  }

  def removeTask(taskId: String) {
    persistenceStore.removeTask(taskId)
  }

    def enqueue(taskId: String, priority: Int) {
      log.fine("Adding task '%s' to queue".format(taskId))
      /* TODO(DLS): something more sane here with different levels */
      val _priority = Math.min(Math.max(-1, priority), 1) + 1
      queues(_priority).add(taskId)
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

  /**
   * Adds a task to the local queue, meaning it will be executed with the next resource offer. The task will be
   * dispatched to mesos as soon as both the task is in the front of the queue and a mesos offer comes in.
   * @param taskId task to run
   */
  def scheduleTask(taskId: String, job: BaseJob, persist: Boolean) {
    scheduleDelayedTask(new ScheduledTask(taskId, DateTime.now(DateTimeZone.UTC), job, this), 0, persist)
  }

  /**
   * Cancels all the taskMappings
   * @param baseJob
   */
  def cancelTasks(baseJob: BaseJob) {
    taskMapping.get(baseJob.name) match {
      case Some(i) => {
        i.foreach({ x =>
          log.info("Cancelling task: " + x._1)
          x._2.cancel(true)
        })
       }
      case None => log.info("No tasks found that need to be cancelled")
    }
    taskMapping -= baseJob.name
    cancelMesosTasks(baseJob)
  }

  /**
   * Removes all tasks from the persistence store that belong to a job.
   * @param baseJob
   */
  def removeTasks(baseJob: BaseJob) {
    log.info("Removing all tasks for job:" + baseJob)
    persistenceStore.getTaskIds(Some(baseJob.name)).foreach({ x =>
      persistenceStore.removeTask(x)
    })
  }

  def cancelMesosTasks(job: BaseJob) {
    import scala.collection.JavaConversions._
    taskCache.asMap
      .filterKeys(TaskUtils.getJobNameForTaskId(_) == job.name)
      .filter(_._2 == TaskState.TASK_RUNNING)
      .foreach({ x =>
      log.warning("Killing task '%s'".format(x._1))
      mesosDriver.get().killTask(TaskID.newBuilder().setValue(x._1).build()) })
  }
}
