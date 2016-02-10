package org.apache.mesos.chronos.scheduler.jobs

import java.util.concurrent.Callable

import org.joda.time.DateTime

/**
 * A wrapper around a task-tuple (taskId, baseJob), which appends the tuple to the job queue once the task is due.
 * @author Florian Leibert (flo@leibert.de)
 */
class ScheduledTask(
                     val taskId: String,
                     val due: DateTime,
                     val job: StoredJob,
                     val taskManager: TaskManager)
  extends Callable[Void] {

  def call(): Void = {
    //TODO(FL): Think about pulling the state updating into the TaskManager.
    taskManager.log.info("Triggering: '%s'".format(job.name))
    taskManager.removeTaskFutureMapping(this)
    taskManager.enqueue(taskId, job.highPriority)
    null
  }
}
