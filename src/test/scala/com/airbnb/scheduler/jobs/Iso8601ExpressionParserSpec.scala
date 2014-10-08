package com.airbnb.scheduler.jobs

import org.joda.time.{Days, Period, DateTime}
import org.specs2.mutable._

class Iso8601ExpressionParserSpec extends SpecificationWithJUnit {
  "Iso8601Expressions$" should {
    "reject non-iso8601 expressions" in {
      Iso8601Expressions.parse("FOO") should be (None)
    }

    "properly parse expression" in {
      val fail = Iso8601Expressions.parse("R5/2008-03-01T13:00:00Z/P1D") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== 5L
          startTime must_== DateTime.parse("2008-03-01T13:00:00Z")
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "properly parse infinite repetition" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime must_== DateTime.parse("2008-03-01T13:00:00Z")
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "properly parse time zone (BST)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00TZ:BST/P1D") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime must_== Iso8601Expressions.convertToDateTime("2008-03-01T13:00:00TZ:BST")
          startTime.getMillis must_== DateTime.parse("2008-03-01T13:00:00+06:00").getMillis
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "properly parse time zone (PST)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00TZ:PST/P1D") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime must_== Iso8601Expressions.convertToDateTime("2008-03-01T13:00:00TZ:PST")
          startTime.getMillis must_== DateTime.parse("2008-03-01T13:00:00-08:00").getMillis
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "Test parse error using two time zone options at once (Z and PST)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00ZTZ:PST/P1D") match {
        case None =>
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "Test parse error with lower case tz" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00tz:PST/P1D") match {
        case None =>
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "Test parse error using two time zone options at once (EDT and PST)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00-04:00TZ:PST/P1D") match {
        case None =>
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

      "Test parse error when period is not specified" in {
        val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00TZ:PST") match {
          case None =>
            false
          case _ =>
            true
        }
        fail must beEqualTo(false)
      }

    "Test parse error when time zone is specified without time" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01TZ:PST") match {
        case None =>
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }



  }
}
