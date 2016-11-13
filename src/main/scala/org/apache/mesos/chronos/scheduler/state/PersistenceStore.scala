package org.apache.mesos.chronos.scheduler.state

import org.apache.mesos.chronos.scheduler.jobs._

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
   * Returns all jobs from the underlying store
   * @return
   */
  def getJobs: Iterator[BaseJob]
}
