package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger

import com.google.inject.Inject
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.Protos._
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.jobs._
import org.apache.mesos.chronos.utils.JobDeserializer
import org.apache.mesos.{Scheduler, SchedulerDriver}
import org.joda.time.DateTime

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Provides the interface to chronos. Receives callbacks from chronos when resources are offered, declined etc.
  *
  * @author Florian Leibert (flo@leibert.de)
  */
class MesosJobFramework @Inject()(
                                   val mesosDriver: MesosDriverFactory,
                                   val scheduler: JobScheduler,
                                   val taskManager: TaskManager,
                                   val config: SchedulerConfiguration,
                                   val frameworkIdUtil: FrameworkIdUtil,
                                   val taskBuilder: MesosTaskBuilder,
                                   val mesosOfferReviver: MesosOfferReviver
                                 )
  extends Scheduler {

  private[this] lazy val declineOfferFilters: Filters = {
    config.declineOfferDuration.get match {
      case Some(durationInMs) =>
        val declineSeconds = durationInMs / 1000.0
        Filters.newBuilder().setRefuseSeconds(declineSeconds).build()
      case None =>
        Filters.getDefaultInstance
    }
  }
  val frameworkName = "chronos"
  private[this] val log = Logger.getLogger(getClass.getName)
  private var lastReconciliation = DateTime.now.plusSeconds(config.reconciliationInterval())

  /* Overridden methods from MesosScheduler */
  @Override
  def registered(schedulerDriver: SchedulerDriver, frameworkID: FrameworkID, masterInfo: MasterInfo) {
    import mesosphere.util.BackToTheFuture.Implicits.defaultTimeout

    import scala.concurrent.ExecutionContext.Implicits.global

    log.info("Registered with ID: " + frameworkID.getValue)
    log.info("Master info:" + masterInfo.toString)
    frameworkIdUtil.store(frameworkID)
    mesosOfferReviver.reviveOffers()
  }

  JobDeserializer.config = config

  /* Overridden methods from MesosScheduler */
  @Override
  def reregistered(schedulerDriver: SchedulerDriver, masterInfo: MasterInfo) {
    log.warning("Reregistered")
    mesosOfferReviver.reviveOffers()
  }

  @Override
  def disconnected(schedulerDriver: SchedulerDriver) {
    log.warning("Disconnected")
  }

  def getReservedResources(offer: Offer): (Double, Double) = {
    val resources = offer.getResourcesList.asScala
    val reservedResources = resources.filter({ x => x.hasRole && x.getRole != "*" })
    (
      getScalarValueOrElse(reservedResources.find(x => x.getName == "cpus"), 0),
      getScalarValueOrElse(reservedResources.find(x => x.getName == "mem"), 0)
    )
  }

  def getScalarValueOrElse(opt: Option[Resource], value: Double): Double = {
    opt.map(x => x.getScalar.getValue).getOrElse(value)
  }

  //TODO(FL): Persist the UPDATED task or job into ZK such that on failover / reload, we don't have to step through the
  //          entire task stream.
  @Override
  def resourceOffers(schedulerDriver: SchedulerDriver, receivedOffers: java.util.List[Offer]) {
    log.fine("Received resource offers")
    import scala.collection.JavaConverters._

    val offers = receivedOffers.asScala.toList
    val offerResources = mutable.HashMap(offers.map(o => (o, Resources(o))).toSeq: _*)
    val tasksToLaunch = generateLaunchableTasks(offerResources)

    log.fine("Declining unused offers.")
    val usedOffers = mutable.HashSet(tasksToLaunch.map(_._3.getId.getValue): _*)

    offers.foreach(o => {
      if (!usedOffers.contains(o.getId.getValue))
        checkDriver(mesosDriver.get.declineOffer(o.getId, declineOfferFilters))
    })

    log.fine(s"Declined unused offers with filter refuseSeconds=${declineOfferFilters.getRefuseSeconds} " +
      s"(use --${config.declineOfferDuration.name} to reconfigure)")

    launchTasks(tasksToLaunch)

    // Perform a reconciliation, if needed.
    reconcile(schedulerDriver)
  }

  def generateLaunchableTasks(offerResources: mutable.HashMap[Offer, Resources]): mutable.Buffer[(String, BaseJob, Offer)] = {
    val tasks = mutable.Buffer[(String, BaseJob, Offer)]()

    @tailrec
    def generate() {
      taskManager.getTaskFromQueue match {
        case None => log.fine("No tasks scheduled or next task has been disabled.\n")
        case Some((taskId, job)) =>
          if (taskManager.jobIsRunning(job.name) && !job.concurrent) {
            log.warning("The head of the task queue appears to already be running and doesn't allow concurrency: " + job.name + "\n")
            generate()
          } else {
            tasks.find(_._2.name == job.name) match {
              case Some((subtaskId, subJob, offer)) =>
                log.warning("Found job in queue that is already scheduled for launch with this offer set: " + subJob.name + "\n")
                generate()
              case None =>
                val neededResources = new Resources(job)
                offerResources.toIterator.find { ors =>
                  ors._2.canSatisfy(neededResources) && ConstraintChecker.checkConstraints(ors._1, job.constraints) && AvailabilityChecker.checkAvailability(ors._1)
                } match {
                  case Some((offer, resources)) =>
                    // Subtract this job's resource requirements from the remaining available resources in this offer.
                    resources -= neededResources
                    tasks.append((taskId, job, offer))
                    generate()
                  case None =>
                    val foundResources = offerResources.toIterator.map(_._2.toString()).mkString(",")
                    log.warning(
                      "Insufficient resources or constraints not met for task '%s', will append to queue. (Needed: [%s], Found: [%s])"
                        .stripMargin.format(taskId, neededResources, foundResources)
                    )
                    taskManager.enqueue(taskId, job.highPriority)
                }
            }
          }
      }
    }

    generate()
    tasks
  }

  def launchTasks(tasks: mutable.Buffer[(String, BaseJob, Offer)]) {
    import scala.collection.JavaConverters._

    tasks.groupBy(_._3).foreach({
      case (offer, subTasks) =>
        val mesosTasks = subTasks.map(task => {
          taskBuilder.getMesosTaskInfoBuilder(task._1, task._2, task._3).setSlaveId(task._3.getSlaveId).build()
        })
        log.info("Launching tasks from offer: " + offer + " with tasks: " + mesosTasks)
        checkDriver(mesosDriver.get.launchTasks(
          List(offer.getId).asJava,
          mesosTasks.asJava
        ))
        for (task <- tasks) {
          val name = task._2.name
          taskManager.addTask(name, task._3.getSlaveId.getValue, task._1)
          scheduler.handleLaunchedTask(task._2)

          log.info(s"Task '${task._1}' launched")
        }
    })
    // After any job schedule may have been updated (after calling `handleLaunchedTask`),
    // we'll have to re-check for jobs that need to run
    scheduler.notifyCondition()
  }

  def checkDriver(status: Status): Unit = {
    if (status != Status.DRIVER_RUNNING) {
      throw new RuntimeException("Driver is no longer running")
    }
  }

  @Override
  def offerRescinded(schedulerDriver: SchedulerDriver, offerID: OfferID) {
    //TODO(FL): Handle this case! In practice this isn't a problem as we have retries.
    log.warning("Offer rescinded for offer:" + offerID.getValue)
  }

  @Override
  def statusUpdate(schedulerDriver: SchedulerDriver, taskStatus: TaskStatus) {
    val taskId = taskStatus.getTaskId.getValue
    taskStatus.getState match {
      case TaskState.TASK_RUNNING =>
        taskManager.updateRunningTask(taskStatus)
        scheduler.handleStartedTask(taskStatus)
      case TaskState.TASK_STAGING =>
        taskManager.updateRunningTask(taskStatus)
        scheduler.handleStartedTask(taskStatus)
      case _ =>
        taskManager.removeTask(taskId)
    }

    taskStatus.getState match {
      case TaskState.TASK_FINISHED =>
        log.info("Task with id '%s' FINISHED".format(taskId))
        scheduler.handleFinishedTask(taskStatus, None)
      case TaskState.TASK_FAILED =>
        log.info("Task with id '%s' FAILED".format(taskId))
        scheduler.handleFailedTask(taskStatus)
      case TaskState.TASK_LOST =>
        log.info("Task with id '%s' LOST".format(taskId))
        scheduler.handleFailedTask(taskStatus)
      case TaskState.TASK_RUNNING =>
        log.info("Task with id '%s' RUNNING".format(taskId))
      case TaskState.TASK_KILLED =>
        log.info("Task with id '%s' KILLED".format(taskId))
        scheduler.handleKilledTask(taskStatus)
      case _ =>
        log.warning("Unknown TaskState:" + taskStatus.getState + " for task: " + taskId)
    }

    // Perform a reconciliation, if needed.
    reconcile(schedulerDriver)
  }

  def reconcile(schedulerDriver: SchedulerDriver): Unit = {
    if (DateTime.now().isAfter(lastReconciliation.plusSeconds(config.reconciliationInterval()))) {
      lastReconciliation = DateTime.now()

      val taskStatuses = taskManager.getAllTaskStatus

      log.info("Performing task reconciliation with the Mesos master")
      schedulerDriver.reconcileTasks(taskStatuses.asJavaCollection)
    }
  }

  @Override
  def frameworkMessage(schedulerDriver: SchedulerDriver, executorID: ExecutorID, slaveID: SlaveID, bytes: Array[Byte]) {
    log.info("Framework message received")
  }

  @Override
  def slaveLost(schedulerDriver: SchedulerDriver, slaveID: SlaveID) {
    log.warning("Slave lost")

    taskManager.removeTasksForSlave(slaveID.getValue)
  }

  @Override
  def executorLost(schedulerDriver: SchedulerDriver, executorID: ExecutorID, slaveID: SlaveID, status: Int) {
    log.info("Executor lost")
  }

  @Override
  def error(schedulerDriver: SchedulerDriver, message: String) {
    log.severe(message)
    scheduler.shutDown()
    System.exit(1)
  }

  private def logOffer(offer: Offer) {
    import scala.collection.JavaConversions._
    val s = new StringBuilder
    offer.getResourcesList.foreach({
      x =>
        s.append(f"Name: ${x.getName}")
        if (x.hasScalar && x.getScalar.hasValue) {
          s.append(f"Scalar: ${x.getScalar.getValue}")
        }
    })

  }

  class Resources(
                   var cpus: Double,
                   var mem: Double,
                   var disk: Double
                 ) {
    def this(job: BaseJob) {
      this(
        if (job.cpus > 0) job.cpus else config.mesosTaskCpu(),
        if (job.mem > 0) job.mem else config.mesosTaskMem(),
        if (job.disk > 0) job.disk else config.mesosTaskDisk()
      )
    }

    def canSatisfy(needed: Resources): Boolean = {
      (this.cpus >= needed.cpus) &&
        (this.mem >= needed.mem) &&
        (this.disk >= needed.disk)
    }

    def -=(that: Resources) {
      this.cpus -= that.cpus
      this.mem -= that.mem
      this.disk -= that.disk
    }

    override def toString: String = {
      "cpus: " + this.cpus + " mem: " + this.mem + " disk: " + this.disk
    }
  }

  object Resources {
    def apply(offer: Offer): Resources = {
      val resources = offer.getResourcesList.asScala.filter(r => !r.hasRole || r.getRole == "*" || r.getRole == config.mesosRole())
      new Resources(
        getScalarValueOrElse(resources.find(_.getName == "cpus"), 0),
        getScalarValueOrElse(resources.find(_.getName == "mem"), 0),
        getScalarValueOrElse(resources.find(_.getName == "disk"), 0)
      )
    }
  }

}
