package org.apache.mesos.chronos.scheduler.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}
import org.apache.mesos.chronos.scheduler.jobs.JobSummaryWrapper

class JobSummaryWrapperSerializer extends JsonSerializer[JobSummaryWrapper] {
  def serialize(jobSummary: JobSummaryWrapper, json: JsonGenerator, provider: SerializerProvider) {
    json.writeStartObject()

    json.writeFieldName("jobs")

    json.writeStartArray()
    for (job <- jobSummary.jobs) {
      json.writeStartObject()

      json.writeFieldName("name")
      json.writeString(job.name)

      json.writeFieldName("status")
      json.writeString(job.status)

      json.writeFieldName("state")
      json.writeString(job.state)

      json.writeFieldName("schedule")
      json.writeString(job.schedule)

      json.writeFieldName("parents")
      json.writeStartArray()
      for (parent <- job.parents) {
        json.writeString(parent)
      }
      json.writeEndArray()

      json.writeFieldName("disabled")
      json.writeBoolean(job.disabled)

      json.writeEndObject()
    }
    json.writeEndArray()

    json.writeEndObject()
  }
}
