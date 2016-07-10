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

object ContainerType extends Enumeration {
  type ContainerType = Value

  // Docker or Mesos
  val DOCKER, MESOS = Value
}

import org.apache.mesos.chronos.scheduler.jobs.NetworkMode._
import org.apache.mesos.chronos.scheduler.jobs.VolumeMode._
import org.apache.mesos.chronos.scheduler.jobs.ContainerType._

case class ExternalVolume(
                           @JsonProperty name: String,
                           @JsonProperty provider: String,
                           @JsonProperty options: Seq[Parameter])

case class PersistentVolume(
                           @JsonProperty size: Int)

case class Volume(
                   @JsonProperty hostPath: Option[String],
                   @JsonProperty containerPath: String,
                   @JsonProperty mode: Option[VolumeMode],
                   @JsonProperty persistent: Option[PersistentVolume],
                   @JsonProperty external: Option[ExternalVolume])

case class Container(
                            @JsonProperty image: String,
                            @JsonProperty `type`: ContainerType = ContainerType.DOCKER,
                            @JsonProperty volumes: Seq[Volume],
                            @JsonProperty parameters: Seq[Parameter],
                            @JsonProperty network: NetworkMode = NetworkMode.HOST,
                            @JsonProperty networkName: Option[String],
                            @JsonProperty forcePullImage: Boolean = false)
