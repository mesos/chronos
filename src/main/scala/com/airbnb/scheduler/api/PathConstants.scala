package com.airbnb.scheduler.api

/**
 * @author Florian Leibert (flo@leibert.de)
 */
object PathConstants {
  final val iso8601JobPath = "/iso8601"
  final val dependentJobPath = "/dependency"
  final val infoPath = "/info"

  final val jobBasePath = "/"
  final val jobPatternPath = "job/{jobName}"
  final val allJobsPath = "jobs"
  final val allStatsPath = "stats/{percentile}"
  final val jobStatsPatternPath = "job/stat/{jobName}"
  final val graphBasePath = "/graph"
  final val jobGraphDotPath = "dot"
  final val jobGraphCsvPath = "csv"
  final val killTaskPattern = "kill/{jobName}"

  final val isMasterPath = "isMaster"
  final val taskBasePath = "/task"
  final val uriTemplate = "http://%s%s"
}
