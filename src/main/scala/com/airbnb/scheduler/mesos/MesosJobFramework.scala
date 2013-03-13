package com.airbnb.scheduler.mesos

import java.util.logging.Logger
import scala.Some

import com.airbnb.scheduler.jobs._
import com.google.inject.Inject
import org.apache.mesos.{Protos, SchedulerDriver, Scheduler}
import org.apache.mesos.Protos._

/**
 * Provides the interface to mesos. Receives callbacks from mesos when resources are offered, declined etc.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosJobFramework @Inject()(
    val mesosDriver: MesosDriverFactory,
    val scheduler: JobScheduler,
    val taskManager: TaskManager)
  extends Scheduler {

  private[this] val log = Logger.getLogger(getClass.getName)

  val frameworkName = "chronos"

  /* Overridden methods from MesosScheduler */
  @Override
  def registered(schedulerDriver: SchedulerDriver, frameworkID: FrameworkID, masterInfo: MasterInfo) {
    log.info("Registered")
    log.info("Master info:" + masterInfo.toString)
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

  //TODO(FL): Persist the UPDATED task or job into ZK such that on failover / reload, we don't have to step through the
  //          entire task stream.
  @Override
  def resourceOffers(schedulerDriver: SchedulerDriver, offers: java.util.List[Offer]) {
    log.info("Received resource offer\n")
    import scala.collection.JavaConverters._
    for (val offer: Offer <- offers.asScala) {
      val opt: Option[(String, BaseJob)] = taskManager.getTask()
      opt match {
        case Some((x, j)) => {
          log.info("Task '%s' scheduled for slave '%s' on host '%s' with offerId: '%s'"
            .format(x, offer.getSlaveId.getValue, offer.getHostname, offer.getId.getValue))
          processTask(x, j, offer)
        }
        case None =>  {
          log.info("No task scheduled! Declining offer:" + offer.getId)
          mesosDriver.get.declineOffer(offer.getId)
        }
      }
    }
  }

  @Override
  def offerRescinded(schedulerDriver: SchedulerDriver, offerID: OfferID) {
    //TODO(FL): Handle this case! In practice this isn't a problem as we have retries.
    log.warning("Offer rescinded for offer:" + offerID.getValue)
  }

  @Override
  def statusUpdate(schedulerDriver: SchedulerDriver, taskStatus: TaskStatus) {
     taskManager.taskCache.put(taskStatus.getTaskId.getValue, taskStatus.getState)

    //TOOD(FL): Add statistics for jobs
    taskStatus.getState match {
      case TaskState.TASK_FINISHED =>
        log.info("Task with id '%s' FINISHED".format(taskStatus.getTaskId.getValue))
        //This is a workaround to support async jobs without having to keep yet more state.
        if (scheduler.isTaskAsync(taskStatus.getTaskId.getValue)) {
          log.info("Asynchronous task: '%s', not updating job-graph.".format(taskStatus.getTaskId.getValue))
        } else {
          scheduler.handleFinishedTask(taskStatus.getTaskId.getValue)
        }
      case TaskState.TASK_FAILED =>
        log.warning("Task with id '%s' FAILED".format(taskStatus.getTaskId.getValue))
        scheduler.handleFailedTask(taskStatus.getTaskId.getValue)
      case TaskState.TASK_LOST =>
        log.warning("Task with id '%s' LOST".format(taskStatus.getTaskId.getValue))
        scheduler.handleFailedTask(taskStatus.getTaskId.getValue)
      case TaskState.TASK_RUNNING =>
        log.warning("Task with id '%s' RUNNING.".format(taskStatus.getTaskId.getValue))
      case _ =>
        log.info("Unknown TaskState:" + taskStatus.getState + " for task: " + taskStatus.getTaskId.getValue)
    }
  }

  @Override
  def frameworkMessage(schedulerDriver: SchedulerDriver, executorID: ExecutorID, slaveID: SlaveID, bytes: Array[Byte]) {
    log.info("Framework message received")
  }

  @Override
  def slaveLost(schedulerDriver: SchedulerDriver, slaveID: SlaveID) {
    //TODO(FL): FIND PENDING TASKS WITH THE GIVEN SLAVE ID
    log.warning("Slave lost")
  }

  @Override
  def executorLost(schedulerDriver: SchedulerDriver, executorID: ExecutorID, slaveID: SlaveID, status: Int) {
    log.info("Executor lost")
  }

  @Override
  def error(schedulerDriver: SchedulerDriver, message: String) {
    log.info("Error: " + message)
    scheduler.stop
  }

  /* END Overridden methods from MesosScheduler */
  /**
   * Processes a task from the local queue.
   * @param taskId
   * @param offer
   */
  def processTask(taskId: String, job: BaseJob, offer: Offer) {
    val taskInfoTemplate = MesosUtils.getMesosTaskInfoBuilder(taskId, job)
    log.fine("Job %s ready for launch at time: %d".format(taskInfoTemplate.getTaskId.getValue,
      System.currentTimeMillis))
    import collection.JavaConversions._

    offer.getResourcesList.map(x =>
      if (x.getType == Value.Type.SCALAR) {
        taskInfoTemplate.addResources(
          Resource.newBuilder().setType(Value.Type.SCALAR).setScalar(
            Protos.Value.Scalar.newBuilder()
              .setValue(x.getScalar.getValue / math.max(x.getScalar.getValue, 1))).setName(x.getName))
      } else {
        log.warning("Ignoring offered resource:" + x.getType)
      })

    val mesosTask = taskInfoTemplate.setSlaveId(offer.getSlaveId).build()

    val filters: Filters = Filters.newBuilder().setRefuseSeconds(0.1).build()

    import scala.collection.JavaConverters._
    val status: Protos.Status = mesosDriver.get.launchTasks(offer.getId, List(mesosTask).asJava, filters)
    if (status == Protos.Status.DRIVER_RUNNING) {
      val deleted = taskManager.removeTask(taskId)
      log.fine("Successfully launched task '%s' via mesos, task records successfully deleted: '%b'"
        .format(taskId, deleted))
    }

    //TODO(FL): Handle case if mesos can't launch the task.
    log.info("Task '%s' launched, status: '%s'".format(taskId, status.toString))
  }
}
