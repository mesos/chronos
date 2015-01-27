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
                            @JsonProperty network: NetworkMode = NetworkMode.HOST)
