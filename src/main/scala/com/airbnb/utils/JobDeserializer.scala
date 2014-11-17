package com.airbnb.utils

import com.airbnb.scheduler.jobs.{BaseJob, DependencyBasedJob, ScheduleBasedJob, Volume, VolumeMode, NetworkMode, DockerContainer}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, DeserializationContext, JsonDeserializer}
import org.joda.time.Period
import scala.collection.JavaConversions._
import com.airbnb.scheduler.config.SchedulerConfiguration
import org.joda.time.{DateTimeZone, DateTime}
import com.airbnb.scheduler.jobs.EnvironmentVariable

object JobDeserializer {
  var config: SchedulerConfiguration = _
}
/**
 * Custom JSON deserializer for jobs.
 * @author Florian Leibert (flo@leibert.de)
 */
class JobDeserializer extends JsonDeserializer[BaseJob] {

  def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): BaseJob = {
    val codec = jsonParser.getCodec

    val node = codec.readTree[JsonNode](jsonParser)

    val name = node.get("name").asText
    val command = node.get("command").asText
    val shell =
      if (node.has("shell") && node.get("shell") != null) node.get("shell").asBoolean
      else true
      
    val epsilon = {
      if (node.has("epsilon")) Period.parse(node.get("epsilon").asText) else Period.seconds(JobDeserializer.config.taskEpsilon())
    }
    val executor =
      if (node.has("executor") && node.get("executor") != null) node.get("executor").asText
      else ""


    val executorFlags =
      if (node.has("executorFlags") && node.get("executorFlags") != null) node.get("executorFlags").asText
      else ""

    val retries =
      if (node.has("retries") && node.get("retries") != null) node.get("retries").asInt
      else 2

    val owner =
      if (node.has("owner") && node.get("owner") != null) node.get("owner").asText
      else ""

    val async =
      if (node.has("async") && node.get("async") != null) node.get("async").asBoolean
      else false

    val disabled =
      if (node.has("disabled") && node.get("disabled") != null) node.get("disabled").asBoolean
      else false

    val softError =
      if (node.has("softError") && node.get("softError") != null) node.get("softError").asBoolean
      else false

    val successCount =
      if (node.has("successCount") && node.get("successCount") != null) node.get("successCount").asLong
      else 0L

    val errorCount =
      if (node.has("errorCount") && node.get("errorCount") != null) node.get("errorCount").asLong
      else 0L

    val lastSuccess =
      if (node.has("lastSuccess") && node.get("lastSuccess") != null) node.get("lastSuccess").asText
      else ""

    val lastError =
      if (node.has("lastError") && node.get("lastError") != null) node.get("lastError").asText
      else ""

    val cpus =
      if (node.has("cpus") && node.get("cpus") != null && node.get("cpus").asDouble != 0) node.get("cpus").asDouble
      else if (JobDeserializer.config != null) JobDeserializer.config.mesosTaskCpu()
      else 0

    val disk =
      if (node.has("disk") && node.get("disk") != null && node.get("disk").asDouble != 0) node.get("disk").asDouble
      else if (JobDeserializer.config != null) JobDeserializer.config.mesosTaskDisk()
      else 0

    val mem =
      if (node.has("mem") && node.get("mem") != null && node.get("mem").asDouble != 0) node.get("mem").asDouble
      else if (JobDeserializer.config != null) JobDeserializer.config.mesosTaskMem()
      else 0

    val errorsSinceLastSuccess =
      if (node.has("errorsSinceLastSuccess") && node.get("errorsSinceLastSuccess") != null)
        node.get("errorsSinceLastSuccess").asLong
      else 0L

    var uris = scala.collection.mutable.ListBuffer[String]()
    if (node.has("uris")) {
      for (uri <- node.path("uris")) {
        uris += uri.asText()
      }
    }
    
    var arguments = scala.collection.mutable.ListBuffer[String]()
    if (node.has("arguments")) {
      for (argument <- node.path("arguments")) {
        arguments += argument.asText()
      }
    }
    
    var environmentVariables = scala.collection.mutable.ListBuffer[EnvironmentVariable]()
    if (node.has("environmentVariables")) {
        node.get("environmentVariables").elements().map {
          case node: ObjectNode => {
            EnvironmentVariable(node.get("name").asText(), node.get("value").asText)
          }
        }.foreach(environmentVariables.add)
    }

    val highPriority =
      if (node.has("highPriority") && node.get("highPriority") != null) node.get("highPriority").asBoolean()
      else false

    val runAsUser =
      if (node.has("runAsUser") && node.get("runAsUser") != null) node.get("runAsUser").asText
      else JobDeserializer.config.user()

    var container: DockerContainer = null
    if (node.has("container")) {
      val containerNode = node.get("container")
      val networkMode =
          if (containerNode.has("network") && containerNode.get("network") != null)
            NetworkMode.withName(containerNode.get("network").asText)
          else NetworkMode.HOST

      // TODO: Add support for more containers when they're added.
      val volumes = scala.collection.mutable.ListBuffer[Volume]()
      if (containerNode.has("volumes")) {
        containerNode.get("volumes").elements().map {
          case node: ObjectNode => {
            val hostPath =
              if (node.has("hostPath")) Option(node.get("hostPath").asText)
              else None
            val mode =
              if (node.has("mode")) Option(VolumeMode.withName(node.get("mode").asText.toUpperCase))
              else None
            Volume(hostPath, node.get("containerPath").asText, mode)
          }
        }.foreach(volumes.add)
      }
      container = DockerContainer(containerNode.get("image").asText, volumes, networkMode)
    }

    var parentList = scala.collection.mutable.ListBuffer[String]()
    if (node.has("parents")) {
      for (parent <- node.path("parents")) {
        parentList += parent.asText
      }
      new DependencyBasedJob(parents = parentList.toSet,
        name = name, command = command, epsilon = epsilon, successCount = successCount, errorCount = errorCount,
        executor = executor, executorFlags = executorFlags, retries = retries, owner = owner, lastError = lastError,
        lastSuccess = lastSuccess, async = async, cpus = cpus, disk = disk, mem = mem, disabled = disabled,
        errorsSinceLastSuccess = errorsSinceLastSuccess, uris = uris, highPriority = highPriority,
        runAsUser = runAsUser, container = container, environmentVariables = environmentVariables, shell = shell,
        arguments = arguments, softError = softError)
    } else if (node.has("schedule")) {
        val scheduleTimeZone = if (node.has("scheduleTimeZone")) node.get("scheduleTimeZone").asText else ""
        new ScheduleBasedJob(node.get("schedule").asText, name = name, command = command,
        epsilon = epsilon, successCount = successCount, errorCount = errorCount, executor = executor,
        executorFlags = executorFlags, retries = retries, owner = owner, lastError = lastError,
        lastSuccess = lastSuccess, async = async, cpus = cpus, disk = disk, mem = mem, disabled = disabled,
        errorsSinceLastSuccess = errorsSinceLastSuccess, uris = uris,  highPriority = highPriority,
        runAsUser = runAsUser, container = container, scheduleTimeZone = scheduleTimeZone,
        environmentVariables = environmentVariables, shell = shell, arguments = arguments, softError = softError)
    } else {
      /* schedule now */
      new ScheduleBasedJob("R1//PT24H", name = name, command = command, epsilon = epsilon, successCount = successCount,
        errorCount = errorCount, executor = executor, executorFlags = executorFlags, retries = retries, owner = owner,
        lastError = lastError, lastSuccess = lastSuccess, async = async, cpus = cpus, disk = disk, mem = mem,
        disabled = disabled, errorsSinceLastSuccess = errorsSinceLastSuccess, uris = uris,  highPriority = highPriority,
        runAsUser = runAsUser, container = container, environmentVariables = environmentVariables, shell = shell,
        arguments = arguments, softError = softError)
    }
  }
}
