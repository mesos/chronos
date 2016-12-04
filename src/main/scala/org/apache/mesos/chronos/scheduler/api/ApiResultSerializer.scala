package org.apache.mesos.chronos.scheduler.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}

class ApiResultSerializer extends JsonSerializer[ApiResult] {
  def serialize(apiResult: ApiResult, json: JsonGenerator, provider: SerializerProvider) {
    json.writeStartObject()

    json.writeFieldName("status")
    json.writeString(apiResult.status)

    json.writeFieldName("message")
    json.writeString(apiResult.message)

    json.writeEndObject()
  }
}
