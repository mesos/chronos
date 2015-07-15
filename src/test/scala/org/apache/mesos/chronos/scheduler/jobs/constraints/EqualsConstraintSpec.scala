package org.apache.mesos.chronos.scheduler.jobs.constraints

import org.specs2.mutable.SpecificationWithJUnit

class EqualsConstraintSpec extends SpecificationWithJUnit
  with ConstraintSpecHelper {

  "matches attributes" in {
    val attributes = List(createTextAttribute("dc", "north"), createTextAttribute("rack", "rack-1"))

    val constraint = EqualsConstraint("rack", "rack-1")

    constraint.matches(attributes) must_== true
  }

  "does not match attributes" in {
    val attributes = List(createTextAttribute("dc", "north"), createTextAttribute("rack", "rack-1"))

    val constraint = EqualsConstraint("rack", "rack-2")

    constraint.matches(attributes) must_== false
  }

}
