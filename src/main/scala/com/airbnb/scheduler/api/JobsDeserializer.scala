package com.airbnb.scheduler.api

import com.airbnb.scheduler.jobs.{BaseJob, DependencyBasedJob, ScheduleBasedJob}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{JsonNode, DeserializationContext, JsonDeserializer}
import org.joda.time.Period
import scala.collection.JavaConversions._

/**
 * Custom JSON deserializer for jobs.
 * @author Florian Leibert (flo@leibert.de)
 */
class JobsDeserializer extends JsonDeserializer[BaseJob] {

  def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): BaseJob = {
    val codec = jsonParser.getCodec

    val node = codec.readTree[JsonNode](jsonParser)

    val name = node.get("name").asText
    val command = node.get("command").asText
    val epsilon = {
      if (node.has("epsilon")) Period.parse(node.get("epsilon").asText) else Period.seconds(60)
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

    var parentList = scala.collection.mutable.ListBuffer[String]()
    if (node.has("parents")) {
      for (parent <- node.path("parents")) {
        parentList += parent.asText
      }
      new DependencyBasedJob(parents = parentList.toSet,
        name = name, command = command, epsilon = epsilon, successCount = successCount, errorCount = errorCount,
        executor = executor, executorFlags = executorFlags, retries = retries, owner = owner, lastError = lastError,
        lastSuccess = lastSuccess, async = async, disabled = disabled)
    } else if (node.has("schedule")) {
      new ScheduleBasedJob(node.get("schedule").asText, name = name, command = command,
        epsilon = epsilon, successCount = successCount, errorCount = errorCount, executor = executor,
        executorFlags = executorFlags, retries = retries, owner = owner, lastError = lastError,
        lastSuccess = lastSuccess, async = async, disabled = disabled)
    } else {
      throw new IllegalStateException("The job found was neither schedule based nor dependency based.")
    }
  }
}
