package org.apache.mesos.chronos.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{JsonNode, DeserializationContext, JsonDeserializer}
import org.apache.mesos.chronos.scheduler.jobs.JobParams

import scala.collection.JavaConversions._

/**
 * Custom JSON deserializer for job params.
 * Example JobParams:
 * {
 *  "arguments": ["A", "B"]
 * }
 */
class JobParamsDeserializer extends JsonDeserializer[JobParams] {
  def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): JobParams = {
    val codec = jsonParser.getCodec
    val node = codec.readTree[JsonNode](jsonParser)
    var arguments = scala.collection.mutable.ListBuffer[String]()

    if (node.has("arguments")) {
      for (argument <- node.path("arguments")) {
        arguments += argument.asText()
      }
    }

    new JobParams(arguments)
  }
}
