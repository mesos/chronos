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
package org.apache.mesos.chronos.scheduler.api

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
  final val jobSearchPath = "jobs/search"
  final val allStatsPath = "stats/{percentile}"
  final val jobStatsPatternPath = "job/stat/{jobName}"
  final val jobTaskProgressPath = "job/{jobName}/task/{taskId}/progress"
  final val graphBasePath = "/graph"
  final val jobGraphDotPath = "dot"
  final val jobGraphCsvPath = "csv"
  final val killTaskPattern = "kill/{jobName}"

  final val isMasterPath = "isMaster"
  final val taskBasePath = "/task"
  final val uriTemplate = "http://%s%s"
}
