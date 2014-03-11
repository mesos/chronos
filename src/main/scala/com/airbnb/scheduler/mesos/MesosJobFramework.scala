package com.airbnb.scheduler.mesos

import java.util.logging.Logger
import scala.Some

import com.airbnb.scheduler.jobs._
import com.airbnb.scheduler.config.SchedulerConfiguration
import com.google.inject.Inject
import org.apache.mesos.{Protos, SchedulerDriver, Scheduler}
import org.apache.mesos.Protos._

import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import mesosphere.mesos.util.FrameworkIdUtil
import com.airbnb.utils.JobDeserializer

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
  private var runningJobs = new HashMap[String, String]
  JobDeserializer.config = config

  val frameworkName = "chronos"

  /* Overridden methods from MesosScheduler */
  @Override
  def registered(schedulerDriver: SchedulerDriver, frameworkID: FrameworkID, masterInfo: MasterInfo) {
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

    val (jobName, _, _) = TaskUtils.parseTaskId(taskStatus.getTaskId.getValue)
    taskStatus.getState match {
      case TaskState.TASK_RUNNING =>
      case _ =>
        runningJobs.remove(jobName)
    }

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
        scheduler.handleFailedTask(taskStatus.getTaskId.getValue, Some(taskStatus.getMessage))
      case TaskState.TASK_LOST =>
        log.warning("Task with id '%s' LOST".format(taskStatus.getTaskId.getValue))
        scheduler.handleFailedTask(taskStatus.getTaskId.getValue, Some(taskStatus.getMessage))
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
    log.warning("Slave lost")

    // Remove any running jobs from this slave
    val jobs = runningJobs.filter {
      case (k, v) =>
        slaveID.getValue == v
    }
    runningJobs --= jobs.keys
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

    var sufficient = scala.collection.mutable.Map[String, Boolean]().withDefaultValue(false)
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
              case y =>
                // not sufficient, skip
            }
          case _ =>
            log.warning("Ignoring offered resource: %s".format(x.getType.toString))
      }})
    (sufficient("cpus") && sufficient("mem") && sufficient("disk"), taskInfoTemplate, offer)
  }

  def processTask(taskId: String, job: BaseJob, offer: Offer, taskInfoTemplate: TaskInfo.Builder) {
    val mesosTask = taskInfoTemplate.setSlaveId(offer.getSlaveId).build()

    val filters: Filters = Filters.newBuilder().setRefuseSeconds(0.1).build()

    log.info("Launching task with offer: " + mesosTask)

    import scala.collection.JavaConverters._
    if (runningJobs.contains(job.name)) {
      log.info("Task '%s' not launched because it appears to be runing".format(taskId))
      mesosDriver.get().declineOffer(offer.getId)
    } else {
      val status: Protos.Status = mesosDriver.get().launchTasks(offer.getId, List(mesosTask).asJava, filters)
      if (status == Protos.Status.DRIVER_RUNNING) {
        val deleted = taskManager.removeTask(taskId)
        log.fine("Successfully launched task '%s' via mesos, task records successfully deleted: '%b'"
          .format(taskId, deleted))
        runningJobs.put(job.name, offer.getSlaveId.getValue)
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
