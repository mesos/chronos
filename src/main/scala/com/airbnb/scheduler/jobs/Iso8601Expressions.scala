package com.airbnb.scheduler.jobs

import org.joda.time.{Period, DateTime, DateTimeZone}
import org.joda.time.format.{ISODateTimeFormat, ISOPeriodFormat}
import java.util.TimeZone

/**
 * Parsing, creating and validation for Iso8601 expressions.
 * @author Florian Leibert (flo@leibert.de)
 */
object Iso8601Expressions {
  private[this] val formatter = ISODateTimeFormat.dateTime
  val iso8601ExpressionRegex = """(R[0-9]*)/(.*)/(P.*)""".r

  /**
   * Parses a ISO8601 expression into a tuple consisting of the number of repetitions (or -1 for infinity),
   * the start and the period.
   * @param input the input string which is a ISO8601 expression consisting of Repetition, Start and Period.
   * @return a three tuple (repetitions, start, period)
   */
  def parse(input: String, timeZoneStr: String = ""): Option[(Long, DateTime, Period)] = {
    try {

      val iso8601ExpressionRegex(repeatStr, startStr, periodStr) = input

      val repeat: Long = {
        if (repeatStr.length == 1)
          -1L
        else
          repeatStr.substring(1).toLong
      }

      val start: DateTime = if (startStr.length == 0) DateTime.now(DateTimeZone.UTC) else convertToDateTime(startStr, timeZoneStr)
      val period: Period = ISOPeriodFormat.standard.parsePeriod(periodStr)
      Some((repeat, start, period))
    } catch {
      case e: scala.MatchError =>
        None
      case e: IllegalArgumentException =>
        None
    }
  }

  /**
   * Verifies that the given expression is a valid Iso8601Expression. Currently not all Iso8601Expression formats
   * are supported.
   * @param input
   * @return
   */
  def canParse(input: String, timeZoneStr: String = ""): Boolean = {
    parse(input, timeZoneStr) match {
      case Some((_, _, _)) =>
        true
      case None =>
        false
    }
  }

  /**
   * Creates a valid Iso8601Expression based on the input parameters.
   * @param recurrences
   * @param startDate
   * @param period
   * @return
   */
  def create(recurrences: Long, startDate: DateTime, period: Period): String = {
    if (recurrences != -1)
      "R%d/%s/%s".format(recurrences, formatter.print(startDate), ISOPeriodFormat.standard.print(period))
    else
      "R/%s/%s".format(formatter.print(startDate), ISOPeriodFormat.standard.print(period))
  }

  /**
   * Creates a DateTime object from an input string.  This parses the object by first checking for a time zone and then
   * using a datetime formatter to format the date and time.
   * @param dateTimeStr the input date time string with optional time zone
   * @return the date time
   */
  def convertToDateTime(dateTimeStr: String, timeZoneStr: String): DateTime = {
    val dateTime = DateTime.parse(dateTimeStr)
    if (timeZoneStr != null && timeZoneStr.length > 0) {
      val timeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZoneStr))
      dateTime.withZoneRetainFields(timeZone)
    } else {
      dateTime
    }
  }
}
