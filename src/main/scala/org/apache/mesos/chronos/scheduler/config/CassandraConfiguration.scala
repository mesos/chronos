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

trait CassandraConfiguration extends ScallopConf {

  lazy val cassandraContactPoints = opt[String]("cassandra_contact_points",
    descr = "Comma separated list of contact points for Cassandra",
    default = None)

  lazy val cassandraPort = opt[Int]("cassandra_port",
    descr = "Port for Cassandra",
    default = Some(9042))

  lazy val cassandraKeyspace = opt[String]("cassandra_keyspace",
    descr = "Keyspace to use for Cassandra",
    default = Some("metrics"))

  lazy val cassandraTable = opt[String]("cassandra_table",
    descr = "Table to use for Cassandra",
    default = Some("chronos"))

  lazy val cassandraStatCountTable = opt[String]("cassandra_stat_count_table",
    descr = "Table to track stat counts in Cassandra",
    default = Some("chronos_stat_count"))

  lazy val cassandraConsistency = opt[String]("cassandra_consistency",
    descr = "Consistency to use for Cassandra",
    default = Some("ANY"))

  lazy val cassandraTtl = opt[Int]("cassandra_ttl",
    descr = "TTL for records written to Cassandra",
    default = Some(3600 * 24 * 365))

  lazy val jobHistoryLimit = opt[Int]("job_history_limit",
    descr = "Number of past job executions to show in history view",
    default = Some(5))
}
