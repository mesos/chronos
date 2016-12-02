package org.apache.mesos.chronos.scheduler.config

import org.rogach.scallop.ScallopConf

trait CassandraConfiguration extends ScallopConf {

  lazy val cassandraContactPoints = opt[String]("cassandra_contact_points",
    descr = "Comma separated list of contact points for Cassandra",
    default = None)

  lazy val cassandraPort = opt[Int]("cassandra_port",
    descr = "Port for Cassandra",
    default = Some(9042))

  lazy val cassandraUser = opt[String]("cassandra_user",
    descr = "User",
    default = None)

  lazy val cassandraPassword = opt[String]("cassandra_password",
    descr = "Password",
    default = None)

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
