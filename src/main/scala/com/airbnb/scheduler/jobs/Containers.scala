package com.airbnb.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty

object VolumeMode extends Enumeration {
  type VolumeMode = Value

  // read-write and read-only.
  val RW, RO = Value
}

import VolumeMode._
case class Volume(
  @JsonProperty hostPath: Option[String],
  @JsonProperty containerPath: String,
  @JsonProperty mode: Option[VolumeMode])

case class DockerContainer(
  @JsonProperty image: String,
  @JsonProperty volumes: Seq[Volume],
  @JsonProperty environmentVariables: Seq[EnvironmentVariable])

case class EnvironmentVariable(
  @JsonProperty name: String,
  @JsonProperty value: String)
