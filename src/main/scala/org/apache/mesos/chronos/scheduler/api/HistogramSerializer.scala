/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mesos.chronos.scheduler.api

import com.codahale.metrics.{ Histogram, Snapshot }
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ JsonSerializer, SerializerProvider }

object HistogramSerializerUtil {
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
  }
}

/**
 * Author: @andykram
 */
class HistogramSerializer extends JsonSerializer[Histogram] {
  def serialize(hist: Histogram, json: JsonGenerator, provider: SerializerProvider) {
    HistogramSerializerUtil.serialize(hist, json, provider)
    json.close()
  }
}
