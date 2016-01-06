package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.mesos.Protos
import org.apache.mesos.Protos.ContainerInfo.DockerInfo
import org.apache.mesos.chronos.scheduler.jobs.Protocol.Protocol
import scala.collection.immutable.Seq

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

object Protocol extends Enumeration {
  type Protocol = Value

  // read-write and read-only.
  val TCP, UDP = Value
}

import org.apache.mesos.chronos.scheduler.jobs.NetworkMode._
import org.apache.mesos.chronos.scheduler.jobs.VolumeMode._

case class Volume(
                   @JsonProperty hostPath: Option[String],
                   @JsonProperty containerPath: String,
                   @JsonProperty mode: Option[VolumeMode])

case class PortMappings(
                   @JsonProperty containerPort: Int,
                   @JsonProperty hostPort: Int,
                   @JsonProperty protocol: Protocol = Protocol.TCP){

}



case class DockerContainer(
                            @JsonProperty image: String,
                            @JsonProperty volumes: Seq[Volume],
                            @JsonProperty network: NetworkMode = NetworkMode.HOST,
                            @JsonProperty forcePullImage: Boolean = false,
                            @JsonProperty portMappings: Option[Seq[PortMappings]] = None)
