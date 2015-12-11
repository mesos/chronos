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
package org.apache.mesos.chronos.scheduler.config

import org.rogach.scallop.ScallopConf

trait GraphiteConfiguration extends ScallopConf {

  lazy val graphiteHostPort = opt[String]("graphite_host_port",
    descr = "Host and port (in the form `host:port`) for Graphite",
    default = None)

  lazy val graphiteReportIntervalSeconds = opt[Long](
    "graphite_reporting_interval",
    descr = "Graphite reporting interval (seconds)",
    default = Some(60L))

  lazy val graphiteGroupPrefix = opt[String]("graphite_group_prefix",
    descr = "Group prefix for Graphite",
    default = Some(""))
}
