package com.airbnb.scheduler.mesos

import java.util.logging.Logger
import scala.Some

import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.config.SchedulerConfiguration
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
    val taskManager: TaskManager,
    val config: SchedulerConfiguration)
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
    log.info("Received resource offers\n")
    import scala.collection.JavaConverters._
    def getNextTask(offers: List[Offer]) {
      taskManager.getTask match {
        case Some((x, j)) => {
          (offers.toIterator.map {
            offer => buildTask(x, j, offer) }.find(_._1)) match {
            case Some((sufficient, taskBuilder, offer)) =>
              processTask(x, j, offer, taskBuilder)
              getNextTask(offers.filter( x => x.getId != offer.getId))
            case _ =>
              log.warning("No sufficient offers found for task '%s', will append to queue".format(x))
              offers.foreach ( offer => mesosDriver.get().declineOffer(offer.getId) )

              /* Put the task back into the queue */
              taskManager.enqueue(x)
          }
        }
        case None => {
          log.info("No tasks scheduled! Declining offers")
          offers.foreach ( offer => mesosDriver.get().declineOffer(offer.getId) )
        }
      }
    }
    getNextTask(offers.asScala.toList)
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
    scheduler.stop()
  }

  def buildTask(taskId: String, job: BaseJob, offer: Offer) : (Boolean, TaskInfo.Builder, Offer) = {
    val taskInfoTemplate = MesosUtils.getMesosTaskInfoBuilder(taskId, job)
    log.fine("Job %s ready for launch at time: %d".format(taskInfoTemplate.getTaskId.getValue,
      System.currentTimeMillis))
    import collection.JavaConversions._

    var sufficient = true

    offer.getResourcesList.foreach(x =>
        x.getType match {
          case Value.Type.SCALAR =>
            (x.getName match {
              case "mem" =>
                config.mesosTaskMem
              case "cpus" =>
                config.mesosTaskCpu
              case "disk" =>
                config.mesosTaskDisk
              case _ =>
                x.getScalar.getValue / math.max(x.getScalar.getValue, 1)
            }) match {
              case value if value <= x.getScalar.getValue =>
                taskInfoTemplate.addResources(
                  Resource.newBuilder().setType(Value.Type.SCALAR).setScalar(
                    Protos.Value.Scalar.newBuilder()
                      .setValue(value)).setName(x.getName))
              case value =>
                log.warning("Insufficient offer, needed %s offered %s: ".format(value.toString, x.getScalar.getValue.toString) + offer)
                sufficient = false
            }
          case _ =>
            log.warning("Ignoring offered resource: %s".format(x.getType.toString))
      })
    (sufficient, taskInfoTemplate, offer)
  }

  def processTask(taskId: String, job: BaseJob, offer: Offer, taskInfoTemplate: TaskInfo.Builder) {
    val mesosTask = taskInfoTemplate.setSlaveId(offer.getSlaveId).build()

    val filters: Filters = Filters.newBuilder().setRefuseSeconds(0.1).build()

    log.info("Launching task with offer: " + mesosTask)

    import scala.collection.JavaConverters._
    val status: Protos.Status = mesosDriver.get().launchTasks(offer.getId, List(mesosTask).asJava, filters)
    if (status == Protos.Status.DRIVER_RUNNING) {
      val deleted = taskManager.removeTask(taskId)
      log.fine("Successfully launched task '%s' via mesos, task records successfully deleted: '%b'"
        .format(taskId, deleted))
    }

    //TODO(FL): Handle case if mesos can't launch the task.
    log.info("Task '%s' launched, status: '%s'".format(taskId, status.toString))
  }
}
