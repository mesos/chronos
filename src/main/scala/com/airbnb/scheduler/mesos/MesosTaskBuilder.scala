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
import org.apache.mesos.Protos.ContainerInfo.DockerInfo

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
    // For a given named resource and value,
    // find and return the role that matches the name and exceeds the value.
    // Give preference to reserved offers first (those whose roles do not match "*")
    import scala.collection.JavaConverters._
    val resources = offer.getResourcesList.asScala
    val reservedResources = resources.filter({x => x.hasRole && x.getRole != "*"})
    val reservedResource = reservedResources.find({x => x.getName == name && x.getScalar.getValue >= value})
    val role = reservedResource match {
      case Some(x) =>
        // We found a good candidate earlier, just use that.
        x.getRole
      case None =>
        // We did not find a good candidate earlier, so use the unreserved role.
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
      if (job.container != null) {
        taskInfo.setContainer(createContainerInfo(job))
      }
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

  def createContainerInfo(job: BaseJob): ContainerInfo = {
    val builder = ContainerInfo.newBuilder()
    job.container.volumes.map { v =>
      val volumeBuilder = Volume.newBuilder().setContainerPath(v.containerPath)
      v.hostPath.map { h =>
        volumeBuilder.setHostPath(h)
      }

      v.mode.map { m =>
        volumeBuilder.setMode(Volume.Mode.valueOf(m.toString.toUpperCase))
      }

      volumeBuilder.build()
    }.foreach(builder.addVolumes)
    builder.setType(ContainerInfo.Type.DOCKER)
    builder.setDocker(DockerInfo.newBuilder().setImage(job.container.image).build()).build
  }

  def getExecutorName(x: String) = "%s".format(x)

  def getDataBytes(executorFlags: String, executorArgs: String) = {
    val dataStr = executorArgsPattern.format(executorFlags, executorArgs)
    ByteString.copyFrom(dataStr.getBytes(Charsets.UTF_8))
  }

  def appendExecutorData(taskInfo: TaskInfo.Builder, job: BaseJob) {
    log.info("Appending executor:" + job.executor + ", flags:" + job.executorFlags + ", command:" + job.command)
    val executor = ExecutorInfo.newBuilder()
      .setExecutorId(ExecutorID.newBuilder().setValue("shell-wrapper-executor"))
      .setCommand(CommandInfo.newBuilder().setValue(job.executor))
    if (job.container != null) {
      executor.setContainer(createContainerInfo(job))
    }
    taskInfo.setExecutor(executor)
      .setData(getDataBytes(job.executorFlags, job.command))
  }
}
