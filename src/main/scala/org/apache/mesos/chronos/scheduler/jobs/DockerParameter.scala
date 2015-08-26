package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty

import org.apache.mesos.{ Protos => mesos }

/**
 * Represents an environment variable definition for the job
 */
case class DockerParameter(
    key: String,
    value: String) {

  def toProto(): mesos.Parameter =
    mesos.Parameter.newBuilder
      .setKey(key)
      .setValue(value)
      .build
}
object DockerParameter {
  def apply(proto: mesos.Parameter): Parameter =
    Parameter(
      proto.getKey,
      proto.getValue
    )
}
