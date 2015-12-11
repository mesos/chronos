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
package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty

object VolumeMode extends Enumeration {
  type VolumeMode = Value

  // read-write and read-only.
  val RW, RO = Value
}

object NetworkMode extends Enumeration {
  type NetworkMode = Value

  // Bridged and Host
  val BRIDGE, HOST = Value
}

import org.apache.mesos.chronos.scheduler.jobs.NetworkMode._
import org.apache.mesos.chronos.scheduler.jobs.VolumeMode._

case class Volume(
  @JsonProperty hostPath: Option[String],
  @JsonProperty containerPath: String,
  @JsonProperty mode: Option[VolumeMode])

case class DockerContainer(
  @JsonProperty image: String,
  @JsonProperty volumes: Seq[Volume],
  @JsonProperty parameters: Seq[Parameter],
  @JsonProperty network: NetworkMode = NetworkMode.HOST,
  @JsonProperty forcePullImage: Boolean = false)
