package com.airbnb.scheduler.api

import com.yammer.metrics.core.Histogram
import com.fasterxml.jackson.databind.{SerializerProvider, JsonSerializer}
import com.fasterxml.jackson.core.JsonGenerator
import com.yammer.metrics.stats.Snapshot

/**
 * Author: @andykram
 */
class HistogramSerializer extends JsonSerializer[Histogram] {
  def serialize(hist: Histogram, json: JsonGenerator, provider: SerializerProvider) {
    val snapshot: Snapshot = hist.getSnapshot
    json.writeStartObject()

    json.writeFieldName("75thPercentile")
    json.writeNumber(snapshot.get75thPercentile())

    json.writeFieldName("95thPercentile")
    json.writeNumber(snapshot.get95thPercentile())

    json.writeFieldName("98thPercentile")
    json.writeNumber(snapshot.get98thPercentile())

    json.writeFieldName("99thPercentile")
    json.writeNumber(snapshot.get99thPercentile())

    json.writeFieldName("median")
    json.writeNumber(snapshot.getMedian)

    json.writeFieldName("mean")
    json.writeNumber(snapshot.getValue(0.5d))

    json.writeFieldName("count")
    json.writeNumber(snapshot.size())

    json.writeEndObject()
    json.close()
  }
}
