package org.apache.mesos.chronos.scheduler.jobs.constraints

import org.apache.mesos.Protos
import org.specs2.mutable.SpecificationWithJUnit

class EqualsConstraintSpec extends SpecificationWithJUnit {

  def createAttribute(name: String, value: String): Protos.Attribute = {
    Protos.Attribute.newBuilder()
      .setName(name)
      .setText(Protos.Value.Text.newBuilder().setValue(value))
      .setType(Protos.Value.Type.TEXT)
      .build()
  }

  "matches attributes" in {
    val attributes = List(createAttribute("dc", "north"), createAttribute("rack", "rack-1"))

    val constraint = EqualsConstraint("rack", "rack-1")

    constraint.matches(attributes) must_== true
  }

  "does not match attributes" in {
    val attributes = List(createAttribute("dc", "north"), createAttribute("rack", "rack-1"))

    val constraint = EqualsConstraint("rack", "rack-2")

    constraint.matches(attributes) must_== false
  }

}
