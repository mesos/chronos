package com.airbnb.scheduler.jobs

import org.joda.time.{Period, DateTime}
import org.joda.time.format.{ISODateTimeFormat, ISOPeriodFormat}

/**
 * Parsing, creating and validation for Iso8601 expressions.
 * @author Florian Leibert (flo@leibert.de)
 */
object Iso8601Expressions {
  private[this] val formatter = ISODateTimeFormat.dateTime;
  val iso8601ExpressionRegex = """(R[0-9]*)/(.*)/(P.*)?""".r

  /**
   * Parses a ISO8601 expression into a tuple consisting of the number of repetitions (or -1 for infinity),
   * the start and the period.
   * @param input the input string which is a ISO8601 expression consisting of Repetition, Start and Period.
   * @return a three tuple (repetitions, start, period)
   */
  def parse(input: String): (Long, DateTime, Period) = {
    val iso8601ExpressionRegex(repeatStr, startStr, periodStr) = input
    val repeat: Long = {
      if (repeatStr.length == 1)
        -1L
      else
        repeatStr.substring(1).toLong
    }

    val start: DateTime = DateTime.parse(startStr)
    val period: Period = ISOPeriodFormat.standard.parsePeriod(periodStr)
    (repeat, start, period)
  }

  /**
   * Verifies that the given expression is a valid Iso8601Expression. Currently not all Iso8601Expression formats
   * are supported.
   * @param input
   * @return
   */
  def canParse(input: String): Boolean = {
    input match {
      case iso8601ExpressionRegex(repeatStr, startStr, periodStr) => return true
      case _ => return false
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
}
