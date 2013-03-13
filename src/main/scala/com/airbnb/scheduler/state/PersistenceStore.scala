package com.airbnb.scheduler.state

import com.airbnb.scheduler.jobs._

/**
 * @author Florian Leibert (flo@leibert.de)
 */
trait PersistenceStore {

  /**
   * Persists a job with the underlying persistence store
   * @param job
   * @return
   */
  def persistJob(job: BaseJob): Boolean

  /**
   * Saves a taskId in the state abstraction.
   * @param name the name of the taks to persist.
   * @param data the data to persist into the task.
   * @return true if the taskId was saved, false if the taskId couldn't be saved.
   */
  def persistTask(name: String, data: Array[Byte]): Boolean

  /**
   * Removes a task from the ZooKeeperState abstraction.
   * @param taskId the taskId to remove.
   * @return true if the job was saved, false if the job couldn't be saved.
   */
  def removeTask(taskId: String): Boolean

  /**
   * Removes a job from the ZooKeeperState abstraction.
   * @param job the job to remove.
   * @return true if the job was saved, false if the job couldn't be saved.
   */
  def removeJob(job: BaseJob)

  /**
   * Loads a job from the underlying store
   * @param name
   * @return
   */
  def getJob(name: String): BaseJob

  /**
   * Purges all tasks from the underlying store
   */
  def purgeTasks()

  /**
   * Returns a list of all task names stored in the underlying store
   * @param filter a filter that's matched on the taskId.
   * @return
   */
  def getTaskIds(filter: Option[String]): List[String]

  /**
   * Gets all tasks from the underlying store
   * @return
   */
  def getTasks(): Map[String, Array[Byte]]

  /**
   * Returns all jobs from the underlying store
   * @return
   */
  def getJobs(): Iterator[BaseJob]
}
