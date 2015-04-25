package org.apache.mesos.chronos.scheduler.jobs.constraints

import org.apache.mesos.Protos

case class EqualsConstraint(attribute: String, value: String) extends Constraint {

  def matches(attributes: Seq[Protos.Attribute]): Boolean = {
    attributes.foreach { a =>
      if (a.getName == attribute && a.getText.getValue == value) {
        return true
      }
    }
    false
  }
}

object EqualsConstraint {
  val OPERATOR = "EQUALS"
}
