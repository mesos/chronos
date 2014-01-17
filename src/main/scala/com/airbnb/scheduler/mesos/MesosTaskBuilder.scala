package com.airbnb.scheduler.mesos

import java.util.logging.Logger

import com.airbnb.scheduler.jobs.BaseJob
import com.google.protobuf.ByteString
import com.google.common.base.Charsets
import org.apache.mesos.Protos._
import org.apache.mesos.Protos.Environment.Variable
import scala.collection.Map
import javax.inject.Inject
import com.airbnb.scheduler.config.SchedulerConfiguration
import scala.collection.JavaConverters._

/**
 * Helpers for dealing dealing with tasks such as generating taskIds based on jobs, parsing them and ensuring that their
 * names are valid.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosTaskBuilder @Inject()(val conf: SchedulerConfiguration) {
  private[this] val log = Logger.getLogger(getClass.getName)
  val taskNameTemplate = "ChronosTask:%s"
  //args|command.
  //  e.g. args: -av (async job), verbose mode
  val executorArgsPattern = "%s|%s"

  final val cpusResourceName = "cpus"
  final val memResourceName = "mem"
  final val diskResourceName = "disk"

  def scalarResource(name: String, value: Double, offer: Offer) = {
    import scala.collection.JavaConverters._
    val role = offer.getResourcesList.asScala.find({x =>
      x.getType match {
        case Value.Type.SCALAR =>
          x.getName == name && x.getScalar.getValue >= value
        case _ =>
          false
      }
    }) match {
      case Some(x) =>
        x.getRole
      case None =>
        "*"
    }
    Resource.newBuilder
      .setName(name)
      .setType(Value.Type.SCALAR)
      .setScalar(Value.Scalar.newBuilder.setValue(value))
      .setRole(role)
      .build
  }

  def scalarResource(name: String, value: Double): Resource = {
    // Added for convenience.  Uses default catch-all role.
    Resource.newBuilder
      .setName(name)
      .setType(Value.Type.SCALAR)
      .setScalar(Value.Scalar.newBuilder.setValue(value))
      .setRole("*")
      .build
  }

  def environment(vars: Map[String, String]) = {
    val builder = Environment.newBuilder()

    for ((key, value) <- vars) {
      val variable = Variable.newBuilder().setName(key).setValue(value)
      builder.addVariables(variable)
    }

    builder.build()
  }

  def getMesosTaskInfoBuilder(taskIdStr: String, job: BaseJob, offer: Offer): TaskInfo.Builder = {
    //TODO(FL): Allow adding more fine grained resource controls.
    val taskId = TaskID.newBuilder().setValue(taskIdStr).build()
    val taskInfo = TaskInfo.newBuilder()
      .setName(taskNameTemplate.format(job.name))
      .setTaskId(taskId)
    val environment = Environment.newBuilder()
      .addVariables(Variable.newBuilder()
        .setName("mesos_task_id").setValue(taskIdStr))
      .addVariables(Variable.newBuilder()
        .setName("CHRONOS_JOB_OWNER").setValue(job.owner))
    if (!job.executor.isEmpty) {
      appendExecutorData(taskInfo, job)
    } else {
      taskInfo.setCommand(
        if (job.command.startsWith("http") || job.command.startsWith("ftp")) {
          val uri1 = CommandInfo.URI.newBuilder()
            .setValue(job.command)
            .setExecutable(true).build()

          CommandInfo.newBuilder().addUris(uri1)
            .setValue("\"." + job.command.substring(
              job.command.lastIndexOf("/")) + "\"")
            .setEnvironment(environment)
        } else {
          val uriProtos = job.uris.map(uri => {
            CommandInfo.URI.newBuilder()
              .setValue(uri)
              .build()
          })
          CommandInfo.newBuilder()
            .setValue(job.command)
            .setEnvironment(environment)
            .addAllUris(uriProtos.asJava)
        })
    }

    val mem = if (job.mem > 0) job.mem else conf.mesosTaskMem()
    val cpus = if (job.cpus > 0) job.cpus else conf.mesosTaskCpu()
    val disk = if (job.disk > 0) job.disk else conf.mesosTaskDisk()
    taskInfo
      .addResources(scalarResource(cpusResourceName, cpus, offer))
      .addResources(scalarResource(memResourceName, mem, offer))
      .addResources(scalarResource(diskResourceName, disk, offer))

    return taskInfo
  }

  def getExecutorName(x: String) = "%s".format(x)

  def getDataBytes(executorFlags: String, executorArgs: String) = {
    val dataStr = executorArgsPattern.format(executorFlags, executorArgs)
    ByteString.copyFrom(dataStr.getBytes(Charsets.UTF_8))
  }

  def appendExecutorData(taskInfo: TaskInfo.Builder, job: BaseJob) {
    log.info("Appending executor:" + job.executor + ", flags:" + job.executorFlags + ", command:" + job.command)
    taskInfo.setExecutor(
      ExecutorInfo.newBuilder()
        .setExecutorId(ExecutorID.newBuilder().setValue("shell-wrapper-executor"))
        .setCommand(CommandInfo.newBuilder().setValue(job.executor)))
      .setData(getDataBytes(job.executorFlags, job.command))
  }
}
