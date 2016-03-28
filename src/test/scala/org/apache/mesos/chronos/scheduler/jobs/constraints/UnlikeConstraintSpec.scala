package org.apache.mesos.chronos.scheduler.jobs.constraints

import java.util.regex.PatternSyntaxException

import org.specs2.mutable.SpecificationWithJUnit

class UnlikeConstraintSpec extends SpecificationWithJUnit
  with ConstraintSpecHelper {
  "matches attributes of type text" in {
    val attributes = List(createTextAttribute("dc", "north"), createTextAttribute("rack", "rack-4"))
    val constraint = UnlikeConstraint("rack", "rack-[1-3]")

    constraint.matches(attributes) must_== true

    val attributes2 = List(createTextAttribute("dc", "north"))
    val constraint2 = UnlikeConstraint("dc", "north|south")

    constraint2.matches(attributes2) must_== false
  }

  "matches attributes of type scalar" in {
    val attributes = List(createScalarAttribute("number", 1))
    val constraint = UnlikeConstraint("number", """\d\.\d""")

    constraint.matches(attributes) must_== false

    val attributes2 = List(createScalarAttribute("number", 1))
    val constraint2 = UnlikeConstraint("number", """100.\d""")
    constraint2.matches(attributes) must_== true

  }

  "matches attributes of type set" in {
    val attributes = List(createSetAttribute("dc", Array("north")))
    val constraint = UnlikeConstraint("dc", "^n.*")

    constraint.matches(attributes) must_== false

    val attributes2 = List(createSetAttribute("dc", Array("south")))

    constraint.matches(attributes2) must_== true
  }

  "fails in case of an invalid regular expression" in {
    UnlikeConstraint("invalid-regex", "[[[") must throwA[PatternSyntaxException]
  }
}
