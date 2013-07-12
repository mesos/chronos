package com.airbnb.scheduler.state

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.logging.{Logger, Level}
import scala.collection.mutable.ListBuffer

import com.airbnb.scheduler.jobs.{JobUtils, DependencyBasedJob, ScheduleBasedJob}
import com.airbnb.scheduler.config.SchedulerConfiguration
import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.ZooKeeperClient
import org.apache.mesos.state.ZooKeeperState

/**
 * This class is used to migrate jobs while the API is evolving.
 */
class Migration(val persistenceStore: MesosStatePersistenceStore) {

  private[this] val log = Logger.getLogger(getClass.getName)

  def migrateJobs() {
    val scheduledJobs = new ListBuffer[ScheduleBasedJob]
    val dependencyBasedJobs = new ListBuffer[DependencyBasedJob]
    //TODO(FL): Remove the migration pieces.
    val asyncPattern = """(.*)(#async)""".r
    import scala.collection.JavaConversions._
    for (f: String <- persistenceStore.state.names.get) {
      if (f.startsWith(persistenceStore.jobPrefix)) {
        log.log(Level.INFO, "Found job in state abstraction: %s".format(f))
        val job = JobUtils.fromBytes(persistenceStore.state.fetch(f).get.value)
        if (job.isInstanceOf[ScheduleBasedJob]) {
          //TODO(FL): (IMPORTANT!) this is a hack to handle a persistence error of the executor.
          val scheduledJob = job.asInstanceOf[ScheduleBasedJob].copy(owner = "flo@airbnb.com")
          //migrate the db

          val copy: ScheduleBasedJob = {
            scheduledJob.name match {
              case asyncPattern(jobName, async) => scheduledJob.copy(name = jobName, async = true)
              case _ => scheduledJob
            }
          }

          log.info("Removing job:" + scheduledJob)
          persistenceStore.removeJob(scheduledJob)
          log.info("Persisting job:" + copy)
          persistenceStore.persistJob(copy)

        } else if (job.isInstanceOf[DependencyBasedJob]) {
          //TODO(FL): (IMPORTANT!) this is a hack to handle a persistence error of the executor.
          val template = job.asInstanceOf[DependencyBasedJob]
          val dependencyJob = template.copy(owner = "flo@airbnb.com", parents = template.parents.map({
            parent =>
              parent match {
                case asyncPattern(jobName, async) => jobName
                case _ => parent
              }
          }))

          val copy = {
            dependencyJob.name match {
              case asyncPattern(jobName, async) => dependencyJob.copy(name = jobName, async = true)
              case _ => dependencyJob
            }
          }
          log.info("Removing job:" + dependencyJob)
          persistenceStore.removeJob(dependencyJob)
          log.info("Persisting job:" + copy)
          persistenceStore.persistJob(copy)
        }
      }
    }
  }
}

object Migration {
  def main(args: Array[String]) {

    val zkServers = new ListBuffer[InetSocketAddress]
    args(0).split(",").map({
      x =>
        require(x.split(":").size == 2, "Error, zookeeper servers must be provided in the form host1:port2,host2:port2")
        zkServers += new InetSocketAddress(x.split(":")(0), x.split(":")(1).toInt)
    })
    val config = new SchedulerConfiguration
    import collection.JavaConversions._
    val zk = new ZooKeeperClient(Amount.of(5000, Time.MILLISECONDS), zkServers)
    val store = new MesosStatePersistenceStore(zk, config, new ZooKeeperState(
      args(0), 5000, TimeUnit.MILLISECONDS, config.zookeeperStateZnode))
    val mig = new Migration(store)
    mig.migrateJobs()
  }
}
