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
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D/TZ:BST") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
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
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D/TZ:PST") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime.getMillis must_== DateTime.parse("2008-03-01T13:00:00-08:00").getMillis
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "Test no time zone change when time zone is not input correctly, i.e. lower tz" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D/tz:PST") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime.getMillis must_==  DateTime.parse("2008-03-01T13:00:00Z").getMillis
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "Test precedence using two time zone options at once (EDT and PST)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00-04:00/P1D/TZ:PST") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime.getMillis must_== DateTime.parse("2008-03-01T13:00:00-08:00").getMillis
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

      "Test parse error when period is not specified" in {
        val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00/TZ:PST") match {
          case None =>
            false
          case _ =>
            true
        }
        fail must beEqualTo(false)
      }

    "Test time zone change when time is not specified" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01/P1D/TZ:PST") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime.getMillis must_== DateTime.parse("2008-03-01T00:00:00-08:00").getMillis
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }



  }
}
