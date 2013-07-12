package com.airbnb.scheduler.jobs

import collection.mutable
import java.util.logging.Logger

import org.joda.time.{DateTimeZone, DateTime}
import com.airbnb.scheduler.state.PersistenceStore

/**
 * This file contains a number of classes and objects for dealing with tasks. Tasks are the actual units of work that
 * get translated into a mesos-task based on a job and it's schedule or as a dependency based on another task. They are
 * serialized to an underlying storage upon submission such that in the case of failed tasks or scheduler failover the
 * task can be retried, double submission during failover is prevented, etc.
 * @author Florian Leibert (flo@leibert.de)
 */

object TaskUtils {

  private[this] val log = Logger.getLogger(getClass.getName)

  //TaskIdFormat: ct:JOB_NAME:DUE:ATTEMPT
  val taskIdTemplate = "ct:%d:%d:%s"
  val taskIdPattern = """ct:(\d+):(\d+):%s""".format(JobUtils.jobNamePattern).r

  def getTaskId(job: BaseJob, due: DateTime, attempt: Int = 0): String = {
   taskIdTemplate.format(due.getMillis, attempt, job.name)
  }

  def parseTaskId(id: String): (String, Long, Int) = {
    val taskIdPattern(due, attempt, jobName) = id
    (jobName, due.toLong, attempt.toInt)
  }

  def isValidVersion(taskIdString: String): Boolean = {
    taskIdPattern.findFirstIn(taskIdString).nonEmpty
  }

  /**
   * Parses the task id into the jobname and the tasks creation time.
   * @param taskId
   * @return
   */
  def getJobNameForTaskId(taskId: String): String = {
    require(taskId != null, "taskId cannot be null")
    try {
      val TaskUtils.taskIdPattern(_, _, jobName) = taskId
      jobName
    } catch {
      case t: Throwable =>
        log.warning("Unable to parse idStr: '%s' due to a corrupted string or version error. " +
          "Warning, dependents will not be triggered!")
        return ""
    }
  }


  def getDueTimes(tasks: Map[String, Array[Byte]]): Map[String, (BaseJob, Long, Int)] = {
    val taskMap = new mutable.HashMap[String, (BaseJob, Long, Int)]()

    tasks.foreach { p: (String, Array[Byte]) => println(p._1)
      //Any non-recurring job R1/X/Y is equivalent to a task!
      val taskInstance = JobUtils.fromBytes(p._2)
      val taskTuple = parseTaskId(p._1)
      val now = DateTime.now(DateTimeZone.UTC).getMillis
      val lastExecutableTime = new DateTime(taskTuple._2, DateTimeZone.UTC).plus(taskInstance.epsilon).getMillis

    //if the task isn't due yet
      if (taskTuple._2 > now) {
        log.fine("Task '%s' is scheduled in the future".format(taskInstance.name))
        taskMap += (p._1 -> (taskInstance, (taskTuple._2 - now), taskTuple._3))
      } else if (lastExecutableTime > now) {
        taskMap += (p._1 -> (taskInstance, 0L, taskTuple._3))
      } else {
        log.fine("Task '%s' is overdue by '%d' ms!".format(p._1, now - taskTuple._2))
        taskMap += (p._1 -> (taskInstance, taskTuple._2 - now, taskTuple._3))
      }
    }

    taskMap.toMap
  }

  def loadTasks(taskManager: TaskManager, persistenceStore: PersistenceStore) {
    val allTasks = persistenceStore.getTasks
    val validTasks = TaskUtils.getDueTimes(allTasks)

    validTasks.foreach({ case (key, valueTuple) =>
      val (job, due, attempt) = valueTuple
      taskManager.removeTask(key)
      if (due == 0L) {
        log.info("Enqeueuing at once")
        taskManager.scheduleTask(TaskUtils.getTaskId(job, DateTime.now(DateTimeZone.UTC), attempt), job, persist = true)
      } else if (due > 0L) {
        log.info("Enqueuing later")
        val newDueTime = DateTime.now(DateTimeZone.UTC).plus(due)
        taskManager.scheduleDelayedTask(
          new ScheduledTask(TaskUtils.getTaskId(job, newDueTime, attempt), newDueTime, job, taskManager), due, persist = true)
      } else {
        log.info(("Filtering out old task '" +
          "%s' overdue by '%d' ms and removing from store.").format(key, due))
      }
    })
  }
}
