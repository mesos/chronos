package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger
import javax.inject.Inject

import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.jobs.{Volume => _, _}
import com.google.common.base.Charsets
import com.google.protobuf.ByteString
import org.apache.mesos.Protos.ContainerInfo.DockerInfo
import org.apache.mesos.Protos.Environment.Variable
import org.apache.mesos.Protos._

import scala.collection.JavaConverters._
import scala.collection.Map

/**
 * Helpers for dealing dealing with tasks such as generating taskIds based on jobs, parsing them and ensuring that their
 * names are valid.
 * @author Florian Leibert (flo@leibert.de)
 */
class MesosTaskBuilder @Inject()(val conf: SchedulerConfiguration) {
  final val cpusResourceName = "cpus"
  final val memResourceName = "mem"
  final val diskResourceName = "disk"
  val taskNameTemplate = "ChronosTask:%s"
  //args|command.
  //  e.g. args: -av (async job), verbose mode
  val executorArgsPattern = "%s|%s"
  private[this] val log = Logger.getLogger(getClass.getName)

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

  def envs(taskIdStr: String, job: BaseJob, offer: Offer): Environment.Builder = {
    val (_, start, attempt, _) = TaskUtils.parseTaskId(taskIdStr)
    val baseEnv = Map(
      "mesos_task_id"           -> taskIdStr,
      "CHRONOS_JOB_OWNER"       -> job.owner,
      "CHRONOS_JOB_NAME"        -> job.name,
      "HOST"                    -> offer.getHostname,
      "CHRONOS_RESOURCE_MEM"    -> job.mem.toString,
      "CHRONOS_RESOURCE_CPU"    -> job.cpus.toString,
      "CHRONOS_RESOURCE_DISK"   -> job.disk.toString,
      "CHRONOS_JOB_RUN_TIME"    -> start.toString,
      "CHRONOS_JOB_RUN_ATTEMPT" -> attempt.toString
    )

    // If the job defines custom environment variables, add them to the builder
    // Don't add them if they already exist to prevent overwriting the defaults
    val finalEnv =
      if (job.environmentVariables != null && job.environmentVariables.nonEmpty) {
        job.environmentVariables.foldLeft(baseEnv)((envs, env) =>
          if (envs.contains(env.name)) envs else envs + (env.name -> env.value)
        )
      } else {
        baseEnv
      }

   finalEnv.foldLeft(Environment.newBuilder())((builder, env) =>
      builder.addVariables(Variable.newBuilder().setName(env._1).setValue(env._2)))
  }

  def getMesosTaskInfoBuilder(taskIdStr: String, job: BaseJob, offer: Offer): TaskInfo.Builder = {
    //TODO(FL): Allow adding more fine grained resource controls.
    val taskId = TaskID.newBuilder().setValue(taskIdStr).build()
    val taskInfo = TaskInfo.newBuilder()
      .setName(taskNameTemplate.format(job.name))
      .setTaskId(taskId)
    val environment = envs(taskIdStr, job, offer)

    val fetch = job.fetch ++ job.uris.map { Fetch(_) }
    val uriCommand = fetch.map { f =>
      CommandInfo.URI.newBuilder()
        .setValue(f.uri)
        .setExtract(f.extract)
        .setExecutable(f.executable)
        .setCache(f.cache)
        .build()
    }

    if (job.executor.nonEmpty) {
      appendExecutorData(taskInfo, job, environment, uriCommand)
    } else {
      val command = CommandInfo.newBuilder()
      if (job.command.startsWith("http") || job.command.startsWith("ftp")) {
        val uri1 = CommandInfo.URI.newBuilder()
          .setValue(job.command)
          .setExecutable(true).build()

        command.addUris(uri1)
          .setValue("\"." + job.command.substring(job.command.lastIndexOf("/")) + "\"")
          .setEnvironment(environment)
      } else {
        command.setValue(job.command)
          .setShell(job.shell)
          .setEnvironment(environment)
          .addAllArguments(job.arguments.asJava)
          .addAllUris(uriCommand.asJava)
      }
      if (job.runAsUser.nonEmpty) {
        command.setUser(job.runAsUser)
      }
      taskInfo.setCommand(command.build())
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

    taskInfo
  }

  def scalarResource(name: String, value: Double, offer: Offer) = {
    // For a given named resource and value,
    // find and return the role that matches the name and exceeds the value.
    // Give preference to reserved offers first (those whose roles do not match "*")
    import scala.collection.JavaConverters._
    val resources = offer.getResourcesList.asScala
    val reservedResources = resources.filter({ x => x.hasRole && x.getRole != "*"})
    val reservedResource = reservedResources.find({ x => x.getName == name && x.getScalar.getValue >= value})
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
      v.external.foreach { e =>
        volumeBuilder.setSource(Volume.Source.newBuilder()
            .setType(Volume.Source.Type.DOCKER_VOLUME)
            .setDockerVolume(Volume.Source.DockerVolume.newBuilder()
              .setDriver(e.provider)
              .setName(e.name)
              .setDriverOptions(Parameters.newBuilder()
                .addAllParameter(e.options.map(_.toProto()).asJava).build()
              ).build()
            ).build()
        ).build()
      }

      volumeBuilder.build()
    }.foreach(builder.addVolumes)

    job.container.`type` match {
      case ContainerType.DOCKER =>
        builder.setType(ContainerInfo.Type.DOCKER)
        builder.setDocker(DockerInfo.newBuilder()
          .setImage(job.container.image)
          .setNetwork(DockerInfo.Network.valueOf(job.container.network.toString.toUpperCase))
          .setForcePullImage(job.container.forcePullImage)
          .addAllParameters(job.container.parameters.map(_.toProto()).asJava)
          .build())
      case ContainerType.MESOS =>
        builder.setType(ContainerInfo.Type.MESOS)
        builder.setMesos(ContainerInfo.MesosInfo.newBuilder()
          .setImage(Image.newBuilder()
            // TODO add APPC image support
            .setType(Image.Type.DOCKER)
            .setDocker(Image.Docker.newBuilder()
              .setName(job.container.image)
              // TODO add setCredential
              .build())
            .setCached(job.container.forcePullImage)
            .build())
          .build())
    }
    job.container.networkName.foreach {
      n => builder.addNetworkInfos(NetworkInfo.newBuilder()
          .setName(n).build()
        )
    }
    builder.build
  }

  private def appendExecutorData(taskInfo: TaskInfo.Builder, job: BaseJob, environment: Environment.Builder, uriProtos: Seq[CommandInfo.URI]) {
    log.info("Appending executor:" + job.executor + ", flags:" + job.executorFlags + ", command:" + job.command)
    val command = CommandInfo.newBuilder()
      .setValue(job.executor)
      .setEnvironment(environment)
      .addAllUris(uriProtos.asJava)
    if (job.runAsUser.nonEmpty) {
      command.setUser(job.runAsUser)
    }
    val executor = ExecutorInfo.newBuilder()
      .setExecutorId(ExecutorID.newBuilder().setValue(job.name))
      .setCommand(command.build())
    if (job.container != null) {
      executor.setContainer(createContainerInfo(job))
    }
    taskInfo.setExecutor(executor).setData(getDataBytes(job))
  }

  private def getDataBytes(job : BaseJob) : ByteString = {
    val string = if (job.taskInfoData != "") {
      job.taskInfoData
    } else {
      executorArgsPattern.format(job.executorFlags, job.command)
    }
    ByteString.copyFrom(string.getBytes(Charsets.UTF_8))
  }
}
