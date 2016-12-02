package org.apache.mesos.chronos.scheduler.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}
import org.apache.mesos.chronos.scheduler.jobs.JobStatWrapper
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, PeriodFormatterBuilder}

class JobStatWrapperSerializer extends JsonSerializer[JobStatWrapper] {
  def serialize(jobStat: JobStatWrapper, json: JsonGenerator, provider: SerializerProvider) {
    json.writeStartObject()

    json.writeFieldName("histogram")
    HistogramSerializerUtil.serialize(jobStat.hist, json, provider)

    val taskStats = jobStat.taskStats
    json.writeFieldName("taskStatHistory")
    json.writeStartArray()
    for (taskStat <- taskStats) {
      json.writeStartObject()

      json.writeFieldName("taskId")
      json.writeString(taskStat.taskId)

      json.writeFieldName("jobName")
      json.writeString(taskStat.jobName)

      json.writeFieldName("slaveId")
      json.writeString(taskStat.taskSlaveId)

      val fmt = DateTimeFormat.forPattern("MM/dd/yy HH:mm:ss")

      def writeTime(timeOpt: Option[DateTime]): Unit =
        timeOpt.fold(json.writeString("N/A"))(dT => json.writeString(fmt.print(dT)))

      //always show start time
      json.writeFieldName("startTime")
      writeTime(taskStat.taskStartTs)
      //show either end time or currently running
      json.writeFieldName("endTime")
      writeTime(taskStat.taskEndTs)

      taskStat.taskDuration.foreach {
        dur =>
          val pFmt = new PeriodFormatterBuilder()
            .appendDays().appendSuffix("d")
            .appendHours().appendSuffix("h")
            .appendMinutes().appendSuffix("m")
            .printZeroIfSupported()
            .appendSeconds().appendSuffix("s")
            .toFormatter

          json.writeFieldName("duration")
          json.writeString(pFmt.print(dur.toPeriod))

      }

      json.writeFieldName("status")
      json.writeString(taskStat.taskStatus.toString)

      //only write elements processed, ignore numAdditionalElementsProcessed
      taskStat.numElementsProcessed.foreach {
        num =>
          json.writeFieldName("numElementsProcessed")
          json.writeNumber(num)
      }

      json.writeEndObject()
    }
    json.writeEndArray()

    json.writeEndObject()
  }
}

