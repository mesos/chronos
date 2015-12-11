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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ JsonSerializer, SerializerProvider }
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
