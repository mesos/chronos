package com.airbnb.scheduler.jobs

import org.joda.time.{Days, Period, DateTime}
import org.specs2.mutable._

class Iso8601ExpressionParserSpec extends SpecificationWithJUnit {
  "Iso8601Expressions$" should {
    "reject non-iso8601 expressions" in {
      Iso8601Expressions.parse("FOO") must throwAn[scala.MatchError]
    }

    "properly parse expression" in {
      val tuple = Iso8601Expressions.parse("R5/2008-03-01T13:00:00Z/P1D")
      tuple._1 must_== 5L
      tuple._2 must_== DateTime.parse("2008-03-01T13:00:00Z")
      //This is a hack because Period's equals seems broken!
      tuple._3.toString must_== new Period(Days.ONE).toString
    }

    "properly parse infinite repetition" in {
      val tuple = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D")
      tuple._1 must_== -1L
      tuple._2 must_== DateTime.parse("2008-03-01T13:00:00Z")
      //This is a hack because Period's equals seems broken!
      tuple._3.toString must_== new Period(Days.ONE).toString
    }

  }
}
