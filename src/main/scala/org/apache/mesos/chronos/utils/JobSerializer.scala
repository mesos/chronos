package org.apache.mesos.chronos.utils

import org.apache.mesos.chronos.scheduler.jobs.{BaseJob, DependencyBasedJob, ScheduleBasedJob}
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}

/**
 * Custom JSON serializer for jobs.
 * @author Florian Leibert (flo@leibert.de)
 */
class JobSerializer extends JsonSerializer[BaseJob] {

  def serialize(baseJob: BaseJob, json: JsonGenerator, provider: SerializerProvider) {
    json.writeStartObject()
    json.writeFieldName("name")
    json.writeString(baseJob.name)

    json.writeFieldName("command")
    json.writeString(baseJob.command)

    json.writeFieldName("shell")
    json.writeBoolean(baseJob.shell)

    json.writeFieldName("epsilon")
    json.writeString(baseJob.epsilon.toString)

    json.writeFieldName("executor")
    json.writeString(baseJob.executor)

    json.writeFieldName("executorFlags")
    json.writeString(baseJob.executorFlags)

    json.writeFieldName("retries")
    json.writeNumber(baseJob.retries)

    json.writeFieldName("owner")
    json.writeString(baseJob.owner)

    json.writeFieldName("async")
    json.writeBoolean(baseJob.async)

    json.writeFieldName("successCount")
    json.writeNumber(baseJob.successCount)

    json.writeFieldName("errorCount")
    json.writeNumber(baseJob.errorCount)

    json.writeFieldName("lastSuccess")
    json.writeString(baseJob.lastSuccess)

    json.writeFieldName("lastError")
    json.writeString(baseJob.lastError)

    json.writeFieldName("cpus")
    json.writeNumber(baseJob.cpus)

    json.writeFieldName("disk")
    json.writeNumber(baseJob.disk)

    json.writeFieldName("mem")
    json.writeNumber(baseJob.mem)

    json.writeFieldName("disabled")
    json.writeBoolean(baseJob.disabled)

    json.writeFieldName("softError")
    json.writeBoolean(baseJob.softError)

    json.writeFieldName("errorsSinceLastSuccess")
    json.writeNumber(baseJob.errorsSinceLastSuccess)

    json.writeFieldName("uris")
    json.writeStartArray()
    baseJob.uris.foreach(json.writeString(_))
    json.writeEndArray()

    json.writeFieldName("environmentVariables")
    json.writeStartArray()
    baseJob.environmentVariables.foreach { v =>
      json.writeStartObject()
      json.writeFieldName("name")
      json.writeString(v.name)
      json.writeFieldName("value")
      json.writeString(v.value)
      json.writeEndObject()
    }
    json.writeEndArray()

    json.writeFieldName("arguments")
    json.writeStartArray()
    baseJob.arguments.foreach(json.writeString(_))
    json.writeEndArray()

    json.writeFieldName("highPriority")
    json.writeBoolean(baseJob.highPriority)

    json.writeFieldName("runAsUser")
    json.writeString(baseJob.runAsUser)

    if (baseJob.container != null) {
      json.writeFieldName("container")
      json.writeStartObject()
      // TODO: Handle more container types when added.
      json.writeFieldName("type")
      json.writeString("docker")
      json.writeFieldName("image")
      json.writeString(baseJob.container.image)
      json.writeFieldName("network")
      json.writeString(baseJob.container.network.toString)
      json.writeFieldName("volumes")
      json.writeStartArray()
      baseJob.container.volumes.foreach { v =>
        json.writeStartObject()
        v.hostPath.map { hostPath =>
          json.writeFieldName("hostPath")
          json.writeString(hostPath)
        }
        json.writeFieldName("containerPath")
        json.writeString(v.containerPath)
        v.mode.map { mode =>
          json.writeFieldName("mode")
          json.writeString(mode.toString)
        }
        json.writeEndObject()
      }
      json.writeEndArray()
      json.writeEndObject()
    }

    if (baseJob.isInstanceOf[DependencyBasedJob]) {
      val depJob = baseJob.asInstanceOf[DependencyBasedJob]
      json.writeFieldName("parents")
      json.writeStartArray()
      depJob.parents.foreach(json.writeString(_))
      json.writeEndArray()
    } else if (baseJob.isInstanceOf[ScheduleBasedJob]) {
      val schedJob = baseJob.asInstanceOf[ScheduleBasedJob]
      json.writeFieldName("schedule")
      json.writeString(schedJob.schedule)
      json.writeFieldName("scheduleTimeZone")
      json.writeString(schedJob.scheduleTimeZone)
    } else {
      throw new IllegalStateException("The job found was neither schedule based nor dependency based.")
    }

    json.writeEndObject()
  }
}
