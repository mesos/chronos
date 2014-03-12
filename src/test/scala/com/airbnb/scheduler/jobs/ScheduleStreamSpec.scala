package com.airbnb.scheduler.jobs

import org.joda.time._
import org.specs2.mutable._

class ScheduleStreamSpec extends SpecificationWithJUnit {

  val fakeCurrentTime = DateTime.parse("2012-01-01T00:00:00Z")

  "ScheduleStream" should {
    "return a properly clipped schedule" in {
      val orgSchedule = "R3/2012-01-01T00:00:00.000Z/P1D"
      val stream = new ScheduleStream(orgSchedule, null)
      stream.head must_== (orgSchedule, null)
      stream.tail.get.head must_== ("R2/2012-01-02T00:00:00.000Z/P1D", null)
      stream.tail.get.tail.get.head must_== ("R1/2012-01-03T00:00:00.000Z/P1D", null)
      stream.tail.get.tail.get.tail.get.head must_== ("R0/2012-01-04T00:00:00.000Z/P1D", null)
      stream.tail.get.tail.get.tail.get.tail must_== None
    }

    "return a infinite schedule when no repetition is specified" in {
      val orgSchedule = "R/2012-01-01T00:00:00.000Z/P1D"
      val stream = new ScheduleStream(orgSchedule, null)
      stream.head must_== (orgSchedule, null)
      stream.tail.get.head must_== ("R/2012-01-02T00:00:00.000Z/P1D", null)
    }
  }

}
