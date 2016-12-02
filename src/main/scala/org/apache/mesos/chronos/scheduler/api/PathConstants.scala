package org.apache.mesos.chronos.scheduler.api

/**
  * @author Florian Leibert (flo@leibert.de)
  */
object PathConstants {
  final val iso8601JobPath = "/iso8601"
  final val dependentJobPath = "/dependency"

  final val jobBasePath = "/"
  final val jobPatternPath = "job/{jobName}"
  final val allJobsPath = "jobs"
  final val jobSummaryPath = "jobs/summary"
  final val jobSearchPath = "jobs/search"
  final val allStatsPath = "stats/{percentile}"
  final val jobStatsPatternPath = "job/stat/{jobName}"
  final val jobTaskProgressPath = "job/{jobName}/task/{taskId}/progress"
  final val jobSuccessPath = "job/success/{jobName}"
  final val graphBasePath = "/graph"
  final val jobGraphDotPath = "dot"
  final val jobGraphCsvPath = "csv"
  final val killTaskPattern = "kill/{jobName}"
  final val getLeaderPattern = "/leader"

  final val isMasterPath = "isMaster"
  final val taskBasePath = "/task"
  final val uriTemplate = "http://%s%s"
}
