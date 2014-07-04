package com.airbnb.utils

import com.airbnb.scheduler.jobs.{Container, ScheduleBasedJob, DependencyBasedJob, BaseJob}
import com.fasterxml.jackson.databind.{SerializerProvider, JsonSerializer}
import com.fasterxml.jackson.core.JsonGenerator

/**
 * Custom JSON serializer for jobs.
 * @author Florian Leibert (flo@leibert.de)
 */
class JobSerializer extends JsonSerializer[BaseJob] {

  def serialize(baseJob: BaseJob , json: JsonGenerator, provider: SerializerProvider) {
    json.writeStartObject()
    json.writeFieldName("name")
    json.writeString(baseJob.name)

    json.writeFieldName("command")
    json.writeString(baseJob.command)

    json.writeFieldName("epsilon")
    json.writeString(baseJob.epsilon.toString)

    json.writeFieldName("executor")
    json.writeString(baseJob.executor)

    json.writeFieldName("executorFlags")
    json.writeString(baseJob.executorFlags)

    json.writeFieldName("container")
    json.writeObject(baseJob.container)

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

    json.writeFieldName("uris")
    json.writeStartArray()
    baseJob.uris.foreach(json.writeString(_))
    json.writeEndArray()

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
    } else {
      throw new IllegalStateException("The job found was neither schedule based nor dependency based.")
    }

    json.writeEndObject()
  }
}

class ContainerSerializer  extends JsonSerializer[Container] {
  def serialize(container: Container , json: JsonGenerator, provider: SerializerProvider) {
    json.writeStartObject()

    json.writeFieldName("image")
    json.writeString(container.image)

    json.writeFieldName("options")
    json.writeStartArray()
    container.options.foreach(json.writeString(_))
    json.writeEndArray()

    json.writeEndObject()
  }
}
