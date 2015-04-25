package org.apache.mesos.chronos.scheduler.jobs.constraints

import org.apache.mesos.Protos

trait Constraint {
  def matches(attributes: Seq[Protos.Attribute]): Boolean
}
