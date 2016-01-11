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

object NetworkProtocol extends Enumeration {
  type NetworkProtocol = Value

  // TCP and UDP
  val TCP, UDP = Value
}

import org.apache.mesos.chronos.scheduler.jobs.NetworkMode._
import org.apache.mesos.chronos.scheduler.jobs.NetworkProtocol._
import org.apache.mesos.chronos.scheduler.jobs.VolumeMode._

case class Volume(
                   @JsonProperty hostPath: Option[String],
                   @JsonProperty containerPath: String,
                   @JsonProperty mode: Option[VolumeMode])

case class PortMapping(
                   @JsonProperty hostPort: Int,
                   @JsonProperty containerPort: Int,
                   @JsonProperty protocol: NetworkProtocol = NetworkProtocol.TCP)

case class DockerContainer(
                            @JsonProperty image: String,
                            @JsonProperty volumes: Seq[Volume],
                            @JsonProperty network: NetworkMode = NetworkMode.HOST,
                            @JsonProperty forcePullImage: Boolean = false,
                            @JsonProperty portMappings: Seq[PortMapping])
