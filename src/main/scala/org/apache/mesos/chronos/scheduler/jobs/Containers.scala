package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty

object VolumeMode extends Enumeration {
  type VolumeMode = Value

  // read-write and read-only.
  val RW, RO = Value
}

object NetworkMode extends Enumeration {
  type NetworkMode = Value

  // Bridged, Host and USER
  val BRIDGE, HOST, USER = Value
}

object ContainerType extends Enumeration {
  type ContainerType = Value

  // Docker, Mesos
  val DOCKER, MESOS = Value
}


object ProtocolType extends Enumeration {
  type ProtocolType = Value

  val IPv4, IPv6 = Value
}

import org.apache.mesos.chronos.scheduler.jobs.NetworkMode._
import org.apache.mesos.chronos.scheduler.jobs.VolumeMode._
import org.apache.mesos.chronos.scheduler.jobs.ContainerType._
import org.apache.mesos.chronos.scheduler.jobs.ProtocolType._

case class ExternalVolume(
                           @JsonProperty name: String,
                           @JsonProperty provider: String,
                           @JsonProperty options: Seq[Parameter])

case class Volume(
                   @JsonProperty hostPath: Option[String],
                   @JsonProperty containerPath: String,
                   @JsonProperty mode: Option[VolumeMode],
                   @JsonProperty external: Option[ExternalVolume])

case class PortMapping(
                        @JsonProperty hostPort: Int,
                        @JsonProperty containerPort: Int,
                        @JsonProperty protocol: Option[String])

case class Network(
                   @JsonProperty name: String,
                   @JsonProperty protocol: Option[ProtocolType],
                   @JsonProperty labels: Seq[Label],
                   @JsonProperty portMappings: Seq[PortMapping])

case class Container(
                            @JsonProperty image: String,
                            @JsonProperty `type`: ContainerType = ContainerType.DOCKER,
                            @JsonProperty volumes: Seq[Volume],
                            @JsonProperty parameters: Seq[Parameter],
                            @JsonProperty network: NetworkMode = NetworkMode.HOST,
                            // DEPRECATED, "networkName" will be removed in a future version.
                            @JsonProperty networkName: Option[String],
                            @JsonProperty networkInfos: Seq[Network],
                            @JsonProperty forcePullImage: Boolean = false)
