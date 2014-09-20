package com.airbnb.scheduler.mesos

import java.util.logging.Logger

import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.config.SchedulerConfiguration
import com.google.inject.Inject
import org.apache.mesos.{Protos, SchedulerDriver, Scheduler}
import org.apache.mesos.Protos._
import org.joda.time.DateTime

import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._
import mesosphere.mesos.util.FrameworkIdUtil
import com.airbnb.utils.JobDeserializer
import scala.math.Ordering.Implicits._
import mesosphere.util.BackToTheFuture

/**
 * Provides the interface to mesos. Receives callbacks from mesos when resources are offered, declined etc.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosJobFramework @Inject()(
    val mesosDriver: MesosDriverFactory,
    val scheduler: JobScheduler,
    val taskManager: TaskManager,
    val config: SchedulerConfiguration,
    val frameworkIdUtil: FrameworkIdUtil,
    val taskBuilder: MesosTaskBuilder)
  extends Scheduler {

  private[this] val log = Logger.getLogger(getClass.getName)

  private class ChronosTask(val slaveId: String,
                            var taskStatus: Option[TaskStatus] = None) {
    override def toString: String = {
      s"slaveId=$slaveId, taskStatus=${taskStatus.getOrElse("none").toString}"
    }
  }

  private var lastReconciliation = DateTime.now.plusSeconds(config.reconciliationInterval())

  private var runningTasks = new HashMap[String, ChronosTask]
  JobDeserializer.config = config

  val frameworkName = "chronos"

  /* Overridden methods from MesosScheduler */
  @Override
  def registered(schedulerDriver: SchedulerDriver, frameworkID: FrameworkID, masterInfo: MasterInfo) {
    import scala.concurrent.ExecutionContext.Implicits.global
    import BackToTheFuture.Implicits.defaultTimeout

    log.info("Registered")
    log.info("Master info:" + masterInfo.toString)
    frameworkIdUtil.store(frameworkID)
  }

  /* Overridden methods from MesosScheduler */
  @Override
  def reregistered(schedulerDriver: SchedulerDriver, masterInfo: MasterInfo) {
    log.warning("Reregistered")
  }

  @Override
  def disconnected(schedulerDriver: SchedulerDriver) {
    log.warning("Disconnected")
  }

  def getScalarValueOrElse(opt: Option[Resource], value: Double): Double = {
    opt.map(x => x.getScalar.getValue).getOrElse(value)
  }

  def getReservedResources(offer: Offer): (Double, Double) = {
    val resources = offer.getResourcesList.asScala
    val reservedResources = resources.filter({x => x.hasRole && x.getRole != "*"})
    (
      getScalarValueOrElse(reservedResources.find(x => x.getName == "cpus"), 0),
      getScalarValueOrElse(reservedResources.find(x => x.getName == "mem"), 0)
    )
  }

  //TODO(FL): Persist the UPDATED task or job into ZK such that on failover / reload, we don't have to step through the
  //          entire task stream.
  @Override
  def resourceOffers(schedulerDriver: SchedulerDriver, offers: java.util.List[Offer]) {
    log.info("Received resource offers\n")
    import scala.collection.JavaConverters._
    def getNextTask(offers: List[Offer]) {
      taskManager.getTask match {
        case Some((x, j)) => {
          (offers.toIterator.map {
            offer => buildTask(x, j, offer) }.find(_._1)) match {
            case Some((isSufficient, taskBuilder, offer)) if isSufficient =>
              processTask(x, j, offer, taskBuilder)
              getNextTask(offers.filter(x => x.getId != offer.getId))
            case _ =>
              log.warning("No sufficient offers found for task '%s', will append to queue".format(x))
              offers.foreach ( offer => mesosDriver.get().declineOffer(offer.getId) )

              /* Put the task back into the queue */
              taskManager.enqueue(x, j.highPriority)
          }
        }
        case _ => {
          log.info("No tasks scheduled! Declining offers")
          offers.foreach ( offer => mesosDriver.get().declineOffer(offer.getId) )
        }
      }
    }

    // Sorting like this ensures that offers with the most amount of
    // reserved resources are preferred first over other offers.
    getNextTask(
      offers.asScala.toList
        .sortWith(getReservedResources(_) > getReservedResources(_))
    )

    // Perform a reconciliation, if needed.
    reconcile(schedulerDriver)
  }

  @Override
  def offerRescinded(schedulerDriver: SchedulerDriver, offerID: OfferID) {
    //TODO(FL): Handle this case! In practice this isn't a problem as we have retries.
    log.warning("Offer rescinded for offer:" + offerID.getValue)
  }

  def reconcile(schedulerDriver: SchedulerDriver): Unit = {
    if (DateTime.now().isAfter(lastReconciliation.plusSeconds(config.reconciliationInterval()))) {
      lastReconciliation = DateTime.now()

      val taskStatuses = runningTasks.keys.flatMap(id => runningTasks.get(id))

      log.info("Performing task reconciliation with the Mesos master")
      schedulerDriver.reconcileTasks(taskStatuses.flatMap(task => task.taskStatus).asJavaCollection)
    }
 }

  def updateRunningTask(jobName: String, taskStatus: TaskStatus): Unit = {
    runningTasks.get(jobName) match {
      case Some(chronosTask) =>
        chronosTask.taskStatus = Some(taskStatus)
      case _ =>
        runningTasks.put(jobName, new ChronosTask(taskStatus.getSlaveId.getValue, Some(taskStatus)))
        log.warning(s"Received status update for untracked jobName=$jobName")
    }
  }

  @Override
  def statusUpdate(schedulerDriver: SchedulerDriver, taskStatus: TaskStatus) {
     taskManager.taskCache.put(taskStatus.getTaskId.getValue, taskStatus.getState)

    val (jobName, _, _) = TaskUtils.parseTaskId(taskStatus.getTaskId.getValue)
    taskStatus.getState match {
      case TaskState.TASK_RUNNING =>
        scheduler.handleStartedTask(taskStatus)
        updateRunningTask(jobName, taskStatus)
      case TaskState.TASK_STAGING =>
        scheduler.handleStartedTask(taskStatus)
        updateRunningTask(jobName, taskStatus)
      case _ =>
        runningTasks.remove(jobName)
    }

    //TOOD(FL): Add statistics for jobs
    taskStatus.getState match {
      case TaskState.TASK_FINISHED =>
        log.info("Task with id '%s' FINISHED".format(taskStatus.getTaskId.getValue))
        //This is a workaround to support async jobs without having to keep yet more state.
        if (scheduler.isTaskAsync(taskStatus.getTaskId.getValue)) {
          log.info("Asynchronous task: '%s', not updating job-graph.".format(taskStatus.getTaskId.getValue))
        } else {
          scheduler.handleFinishedTask(taskStatus)
        }
      case TaskState.TASK_FAILED =>
        log.info("Task with id '%s' FAILED".format(taskStatus.getTaskId.getValue))
        scheduler.handleFailedTask(taskStatus)
      case TaskState.TASK_LOST =>
        log.info("Task with id '%s' LOST".format(taskStatus.getTaskId.getValue))
        scheduler.handleFailedTask(taskStatus)
      case TaskState.TASK_RUNNING =>
        log.info("Task with id '%s' RUNNING.".format(taskStatus.getTaskId.getValue))
      case _ =>
        log.warning("Unknown TaskState:" + taskStatus.getState + " for task: " + taskStatus.getTaskId.getValue)
    }

    // Perform a reconciliation, if needed.
    reconcile(schedulerDriver)
  }

  @Override
  def frameworkMessage(schedulerDriver: SchedulerDriver, executorID: ExecutorID, slaveID: SlaveID, bytes: Array[Byte]) {
    log.info("Framework message received")
  }

  @Override
  def slaveLost(schedulerDriver: SchedulerDriver, slaveID: SlaveID) {
    log.warning("Slave lost")

    // Remove any running jobs from this slave
    val jobs = runningTasks.filter {
      case (k, v) =>
        slaveID.getValue == v.slaveId
    }
    runningTasks --= jobs.keys
  }

  @Override
  def executorLost(schedulerDriver: SchedulerDriver, executorID: ExecutorID, slaveID: SlaveID, status: Int) {
    log.info("Executor lost")
  }

  @Override
  def error(schedulerDriver: SchedulerDriver, message: String) {
    log.info("Error: " + message)
    scheduler.shutDown()
  }

  /**
   * Builds a task
   * @param taskId
   * @param job
   * @param offer
   * @return and returns a tuple containing a boolean indicating if sufficient
   *         resources where offered, the TaskBuilder and the offer.
   */
  def buildTask(taskId: String, job: BaseJob, offer: Offer) : (Boolean, TaskInfo.Builder, Offer) = {
    val taskInfoTemplate = taskBuilder.getMesosTaskInfoBuilder(taskId, job, offer)
    log.fine("Job %s ready for launch at time: %d".format(taskInfoTemplate.getTaskId.getValue,
      System.currentTimeMillis))
    import collection.JavaConversions._

    val sufficient = scala.collection.mutable.Map[String, Boolean]().withDefaultValue(false)
    logOffer(offer)
    offer.getResourcesList.foreach({x =>
        log.info(x.getScalar.getValue.getClass.getName)
        x.getType match {
          case Value.Type.SCALAR =>
            val amount = x.getName match {
              case "mem" =>
                if (job.mem > 0) job.mem else config.mesosTaskMem()
              case "cpus" =>
                if (job.cpus > 0) job.cpus else config.mesosTaskCpu()
              case "disk" =>
                if (job.disk > 0) job.disk else config.mesosTaskDisk()
              case _ =>
                x.getScalar.getValue / math.max(x.getScalar.getValue, 1)
            }

            x.getScalar.getValue match {

              case value: Double => {
                if (value.doubleValue() >= amount && !sufficient(x.getName)) {
                  sufficient(x.getName) = true
                }
              }
            }
          case _ =>
            log.warning("Ignoring offered resource: %s".format(x.getType.toString))
      }})
    (sufficient("cpus") && sufficient("mem") && sufficient("disk"), taskInfoTemplate, offer)
  }

  def processTask(taskId: String, job: BaseJob, offer: Offer, taskInfoTemplate: TaskInfo.Builder) {
    val mesosTask = taskInfoTemplate.setSlaveId(offer.getSlaveId).build()

    val filters: Filters = Filters.newBuilder().setRefuseSeconds(0.1).build()

    log.info("Launching task from offer: " + offer + " with task: " + mesosTask)

    import scala.collection.JavaConverters._
    if (runningTasks.contains(job.name)) {
      log.info("Task '%s' not launched because it appears to be runing".format(taskId))
      mesosDriver.get().declineOffer(offer.getId)
    } else {
      val status: Protos.Status =
        mesosDriver.get().launchTasks(
        List(offer.getId).asJava,
        List(mesosTask).asJava,
        filters
      )
      if (status == Protos.Status.DRIVER_RUNNING) {
        val deleted = taskManager.removeTask(taskId)
        log.fine("Successfully launched task '%s' via mesos, task records successfully deleted: '%b'"
          .format(taskId, deleted))
        runningTasks.put(job.name, new ChronosTask(offer.getSlaveId.getValue))
      }

      //TODO(FL): Handle case if mesos can't launch the task.
      log.info("Task '%s' launched, status: '%s'".format(taskId, status.toString))
    }
  }

  private def logOffer(offer : Offer) {
    import collection.JavaConversions._
    val s = new StringBuilder
    offer.getResourcesList.foreach({
      x => s.append(f"Name: ${x.getName}")
      if (x.hasScalar && x.getScalar.hasValue) {
        s.append(f"Scalar: ${x.getScalar.getValue}")
      }
    })

  }
}
