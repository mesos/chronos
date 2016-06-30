package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger
import javax.inject.Inject

import mesosphere.mesos.protos.RangesResource
import org.apache.mesos.Protos.ContainerInfo.DockerInfo.PortMapping
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.jobs.{BaseJob, DependencyBasedJob, DockerContainer, JobScheduler, ScheduleBasedJob}
import com.google.common.base.Charsets
import com.google.protobuf.ByteString
import org.apache.mesos.Protos
import org.apache.mesos.Protos.ContainerInfo.DockerInfo
import org.apache.mesos.Protos.Environment.Variable
import org.apache.mesos.Protos._
import org.apache.mesos.chronos.utils.PortsMatcher

import scala.collection.JavaConverters._
import scala.collection.Map

/**
 * Helpers for dealing dealing with tasks such as generating taskIds based on jobs, parsing them and ensuring that their
 * names are valid.
  *
  * @author Florian Leibert (flo@leibert.de)
 */
class MesosTaskBuilder @Inject()(val conf: SchedulerConfiguration, val scheduler: JobScheduler) {
  import mesosphere.mesos.protos.Implicits._

  final val cpusResourceName = "cpus"
  final val memResourceName = "mem"
  final val diskResourceName = "disk"
  final val portsResources = "ports"

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
      .addVariables(Variable.newBuilder()
      .setName("CHRONOS_JOB_NAME").setValue(job.name))
      .addVariables(Variable.newBuilder()
      .setName("HOST").setValue(offer.getHostname))
      .addVariables(Variable.newBuilder()
      .setName("CHRONOS_RESOURCE_MEM").setValue(job.mem.toString))
      .addVariables(Variable.newBuilder()
      .setName("CHRONOS_RESOURCE_CPU").setValue(job.cpus.toString))
      .addVariables(Variable.newBuilder()
      .setName("CHRONOS_RESOURCE_DISK").setValue(job.disk.toString))

    val portsMatcher = new PortsMatcher(job, offer)
    val portsOpt: Option[Seq[RangesResource]] = portsMatcher.portRanges
    val ports = portsOpt.map {
      _.flatMap(_.ranges.flatMap(_.asScala()).to[Seq])
    }

    ports.map { u =>
      val containerPorts = for (pms <- job.portMappings) yield pms.map(_.containerPort)
      val declaredPorts = containerPorts.getOrElse(job.ports)
      val portsEnvMap: Map[String, String] = portsEnv(declaredPorts, u).toMap
      portsEnvMap.map(env =>
        environment.addVariables(Variable.newBuilder().setName(env._1).setValue(env._2))
      )
    }

    // If the job defines custom environment variables, add them to the builder
    // Don't add them if they already exist to prevent overwriting the defaults
    val builtinEnvNames = environment.getVariablesList.asScala.map(_.getName).toSet
    if (job.environmentVariables != null && job.environmentVariables.nonEmpty) {
      job.environmentVariables.foreach(env =>
        if (!builtinEnvNames.contains(env.name)) {
          environment.addVariables(Variable.newBuilder().setName(env.name).setValue(env.value))
        }
      )
    }

    if (job.executor.nonEmpty) {
      appendExecutorData(taskInfo, job, offer)
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
        val uriProtos = job.uris.map(uri => {
          CommandInfo.URI.newBuilder()
            .setValue(uri)
            .build()
        })
        command.setValue(job.command)
          .setShell(job.shell)
          .setEnvironment(environment)
          .addAllArguments(job.arguments.asJava)
          .addAllUris(uriProtos.asJava)
      }
      if (job.runAsUser.nonEmpty) {
        command.setUser(job.runAsUser)
      }
      taskInfo.setCommand(command.build())

      //TODO refactor
      val newJob = job match {
        case job: DependencyBasedJob =>
          job.copy(lastHost = offer.getHostname, currentPorts = if(ports.isDefined) ports.get.map(_.toInt) else List())
        case job: ScheduleBasedJob =>
          job.copy(lastHost = offer.getHostname, currentPorts = if(ports.isDefined) ports.get.map(_.toInt) else List())
      }
      scheduler.replaceJob(job, newJob)

      if (job.container != null) {
        if (portsOpt.nonEmpty) {
          portsOpt.get.foreach(taskInfo.addResources(_))
        }
        taskInfo.setContainer(createContainerInfo(newJob, offer, portsOpt))
      }
    }

    //add discoveryInfo
    val discoveryInfoBuilder = Protos.DiscoveryInfo.newBuilder()
    discoveryInfoBuilder.setName(job.name)
    discoveryInfoBuilder.setVisibility(org.apache.mesos.Protos.DiscoveryInfo.Visibility.FRAMEWORK)

    val portsBuilder = Protos.Ports.newBuilder()
    ports.get.map { u =>
        portsBuilder.addPorts(Protos.Port.newBuilder().setProtocol("tcp").setNumber(u.toInt))
    }

    discoveryInfoBuilder.setPorts(portsBuilder)
    taskInfo.setDiscovery(discoveryInfoBuilder)

    val mem = if (job.mem > 0) job.mem else conf.mesosTaskMem()
    val cpus = if (job.cpus > 0) job.cpus else conf.mesosTaskCpu()
    val disk = if (job.disk > 0) job.disk else conf.mesosTaskDisk()
    taskInfo
      .addResources(scalarResource(cpusResourceName, cpus, offer))
      .addResources(scalarResource(memResourceName, mem, offer))
      .addResources(scalarResource(diskResourceName, disk, offer))

    taskInfo
  }

  protected def computeContainerInfo(job: BaseJob, ports: Seq[Long]): DockerContainer = {
    if (job.container == null) null
    else {
      // Fill in Docker container details if necessary
      val c = job.container

      val portMappings = c.portMappings.map { pms =>
          pms zip ports map {
            case (mapping, port) =>
              // Use case: containerPort = 0 and hostPort = 0
              //
              // For apps that have their own service registry and require p2p communication,
              // they will need to advertise
              // the externally visible ports that their components come up on.
              // Since they generally know there container port and advertise that, this is
              // fixed most easily if the container port is the same as the externally visible host
              // port.
              if (mapping.containerPort == 0) {
                mapping.copy(hostPort = port.toInt, containerPort = port.toInt)
              }
              else {
                mapping.copy(hostPort = port.toInt)
              }
          }
      }

      portMappings match {
        case None => c
        case Some(newMappings) => c.copy(portMappings = Option(newMappings))
      }

      //        builder.mergeFrom(containerWithPortMappings.toMesos())

      // Set NetworkInfo if necessary
//      job.ipAddress.foreach { ipAddress =>
//        val ipAddressLabels = Labels.newBuilder().addAllLabels(ipAddress.labels.map {
//          case (key, value) => Label.newBuilder.setKey(key).setValue(value).build()
//        }.asJava)
//        val networkInfo: NetworkInfo.Builder =
//          NetworkInfo.newBuilder()
//            .addAllGroups(ipAddress.groups.asJava)
//            .setLabels(ipAddressLabels)
//            .addIpAddresses(NetworkInfo.IPAddress.getDefaultInstance)
//        builder.addNetworkInfos(networkInfo)
//      }
    }
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

  def createContainerInfo(job: BaseJob, offer: Offer,portsOpt: Option[Seq[RangesResource]]): ContainerInfo = {

    val ports = portsOpt.map {
      _.flatMap(_.ranges.flatMap(_.asScala()).to[Seq])
    }

    val container: Option[DockerContainer] = if(ports.nonEmpty) Option(computeContainerInfo(job, ports.get)) else None

    //TODO refactor
    var currentJob: BaseJob = job
    if (container.isDefined) {
      val newJob = job match {
        case job: DependencyBasedJob =>
          job.copy(container = container.get, lastHost = offer.getHostname)
        case job: ScheduleBasedJob =>
          job.copy(container = container.get, lastHost = offer.getHostname)
      }
      scheduler.replaceJob(job, newJob)
      currentJob = newJob
    }

    val builder = ContainerInfo.newBuilder()
    currentJob.container.volumes.map { v =>
      val volumeBuilder = Volume.newBuilder().setContainerPath(v.containerPath)
      v.hostPath.map { h =>
        volumeBuilder.setHostPath(h)
      }

      v.mode.map { m =>
        volumeBuilder.setMode(Volume.Mode.valueOf(m.toString.toUpperCase))
      }

      volumeBuilder.build()
    }.foreach(builder.addVolumes)

    val dockerBuilder = DockerInfo.newBuilder()

    currentJob.container.portMappings.get.map { p =>
      val portMappingBuilder = PortMapping.newBuilder().setContainerPort(p.containerPort)
      portMappingBuilder.setHostPort(p.hostPort)
      portMappingBuilder.setProtocol(p.protocol.toString)


      portMappingBuilder.build()
    }.foreach(dockerBuilder.addPortMappings)

    builder.setType(ContainerInfo.Type.DOCKER)
    builder.setDocker(dockerBuilder
      .setImage(currentJob.container.image)
      .setNetwork(DockerInfo.Network.valueOf(currentJob.container.network.toString.toUpperCase))
      .setForcePullImage(currentJob.container.forcePullImage)
      .build()).build
  }

  def appendExecutorData(taskInfo: TaskInfo.Builder, job: BaseJob, offer: Offer) {
    log.info("Appending executor:" + job.executor + ", flags:" + job.executorFlags + ", command:" + job.command)
    val command = CommandInfo.newBuilder()
      .setValue(job.executor)
    if (job.runAsUser.nonEmpty) {
      command.setUser(job.runAsUser)
    }
    val executor = ExecutorInfo.newBuilder()
      .setExecutorId(ExecutorID.newBuilder().setValue("shell-wrapper-executor"))
      .setCommand(command.build())
    if (job.container != null) {
      val portsMatcher = new PortsMatcher(job, offer)
      val portsOpt: Option[Seq[RangesResource]] = portsMatcher.portRanges

      //TODO refactor
      val newJob = job match {
        case job: DependencyBasedJob =>
          job.copy(lastHost = offer.getHostname)
        case job: ScheduleBasedJob =>
          job.copy(lastHost = offer.getHostname)
      }
      scheduler.replaceJob(job, newJob)

      executor.setContainer(createContainerInfo(job, offer, portsOpt))
    }
    taskInfo.setExecutor(executor)
      .setData(getDataBytes(job.executorFlags, job.command))
  }

  def getDataBytes(executorFlags: String, executorArgs: String) = {
    val dataStr = executorArgsPattern.format(executorFlags, executorArgs)
    ByteString.copyFrom(dataStr.getBytes(Charsets.UTF_8))
  }

  def getExecutorName(x: String) = "%s".format(x)

  def portsEnv(definedPorts: Seq[Int], assignedPorts: Seq[Long]): Map[String, String] = {
    if (assignedPorts.isEmpty) {
      Map.empty
    }
    else {
      val env = Map.newBuilder[String, String]

      assignedPorts.zipWithIndex.foreach {
        case (p, n) =>
          env += (s"PORT$n" -> p.toString)
      }

      definedPorts.zip(assignedPorts).foreach {
        case (defined, assigned) =>
          if (defined != 0) {
            env += (s"PORT_$defined" -> assigned.toString)
          }
      }

      env += ("PORT" -> assignedPorts.head.toString)
      env += ("PORTS" -> assignedPorts.mkString(","))
      env.result()
    }
  }
}
