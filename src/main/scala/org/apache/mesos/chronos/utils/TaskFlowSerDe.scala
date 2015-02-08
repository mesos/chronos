package org.apache.mesos.chronos.utils

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import org.apache.mesos.chronos.scheduler.jobs.TaskFlowState

/**
 * Serializer for TaskFlowState. It may be better to use a proper scala jackson library.
 */
object taskFlowSerde {

  val mapper = new ObjectMapper
  val mod = new SimpleModule("TaskFlowStateModule")
  mod.addSerializer(classOf[TaskFlowState], new TaskFlowSerializer)
  mod.addDeserializer(classOf[TaskFlowState], new TaskFlowDeserializer)
  mapper.registerModule(mod)
  
  def serString(flowState: TaskFlowState) = mapper.writeValueAsString(flowState)
  def serBytes(flowState: TaskFlowState) = mapper.writeValueAsBytes(flowState)

  def deserialize(bytes: Array[Byte]) = mapper.readValue(bytes, classOf[TaskFlowState])
  
  class TaskFlowSerializer extends JsonSerializer[TaskFlowState]{
    override def serialize(t: TaskFlowState, json: JsonGenerator, 
                           serializerProvider: SerializerProvider): Unit = {
      json.writeStartObject()
      json.writeFieldName("id")
      json.writeString(t.id)
      
      json.writeFieldName("env")
      json.writeStartObject()
      t.env map {case (key, value) =>
        json.writeFieldName(key)
        json.writeString(value)
      }
      json.writeEndObject()
      json.writeEndObject()
    }
  }

  class TaskFlowDeserializer extends JsonDeserializer[TaskFlowState] {
    override def deserialize(jsonParser: JsonParser, 
                             deserializationContext: DeserializationContext): TaskFlowState = {
      val codec = jsonParser.getCodec
      val node = codec.readTree[JsonNode](jsonParser)

      val id = node.get("id").asText
      val elements = node.get("env").elements()
      val fields = node.get("env").fields()
      var env : Map[String, String] = Map()
      while (fields.hasNext) {
        val next = fields.next()
        env = env + (next.getKey -> next.getValue.asText())
      }
      new TaskFlowState(id, env)
    }
  }
}
