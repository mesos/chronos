package org.apache.mesos.chronos.scheduler.jobs

import org.joda.time.{DateTime, Days, Period, Seconds}
import org.specs2.mutable._

class Iso8601ExpressionParserSpec extends SpecificationWithJUnit {
  "Iso8601Expressions$" should {
    "reject non-iso8601 expressions" in {
      Iso8601Expressions.parse("FOO") should be(None)
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


    "empty schedule parses" in {
      val fail = Iso8601Expressions.parse("R1//P1D") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== 1L
          Seconds.secondsBetween(DateTime.now(), startTime).getSeconds must be_<=(60)
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "properly parse time zone (BST)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D", "BST") match {
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
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D", "PST") match {
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

    "properly parse time zone (Europe/Paris)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z/P1D", "Europe/Paris") match {
        case Some((repetitions, startTime, period)) =>
          repetitions must_== -1L
          startTime.getMillis must_== DateTime.parse("2008-03-01T13:00:00+01:00").getMillis
          //This is a hack because Period's equals seems broken!
          period.toString must_== new Period(Days.ONE).toString
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }


    "Test precedence using two time zone options at once (EDT and PST)" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00-04:00/P1D", "PST") match {
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
      val fail = Iso8601Expressions.parse("R/2008-03-01T13:00:00Z", "PST") match {
        case None =>
          false
        case _ =>
          true
      }
      fail must beEqualTo(false)
    }

    "Test time zone change when time is not specified" in {
      val fail = Iso8601Expressions.parse("R/2008-03-01/P1D", "PST") match {
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
