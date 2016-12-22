package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.{Protos => mesos}

/**
  * Represents an environment variable definition for the job
  */
case class Label(
                      key: String,
                      value: String) {

  def toProto(): mesos.Label =
    mesos.Label.newBuilder
      .setKey(key)
      .setValue(value)
      .build
}

object Label {
  def apply(proto: mesos.Label): Label =
    Label(
      proto.getKey,
      proto.getValue
    )
}
