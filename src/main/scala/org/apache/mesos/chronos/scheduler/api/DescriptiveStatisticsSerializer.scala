package org.apache.mesos.chronos.scheduler.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

/**
 * Serializes a subset of the fields of DescriptiveStatistics
 * @author Florian Leibert (flo@leibert.de)
 */
@Deprecated
class DescriptiveStatisticsSerializer extends JsonSerializer[DescriptiveStatistics] {

  def serialize(stat: DescriptiveStatistics, json: JsonGenerator, provider: SerializerProvider) {
    json.writeStartObject()

    json.writeFieldName("75thPercentile")
    json.writeNumber(stat.getPercentile(75))

    json.writeFieldName("95thPercentile")
    json.writeNumber(stat.getPercentile(95))

    json.writeFieldName("98thPercentile")
    json.writeNumber(stat.getPercentile(98))

    json.writeFieldName("99thPercentile")
    json.writeNumber(stat.getPercentile(99))

    json.writeFieldName("median")
    json.writeNumber(stat.getPercentile(50))

    json.writeFieldName("mean")
    json.writeNumber(stat.getMean)

    json.writeFieldName("count")
    json.writeNumber(stat.getN)

    json.writeEndObject()
    json.close()
  }
}
