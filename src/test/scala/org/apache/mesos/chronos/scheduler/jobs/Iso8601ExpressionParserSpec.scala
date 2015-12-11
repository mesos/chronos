/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mesos.chronos.scheduler.jobs

import org.joda.time.{ DateTime, Days, Period }
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
