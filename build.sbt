name := "chronos"

organization := "org.apache.mesos"

version := "2.5.0-SNAPSHOT"

scalaVersion := "2.11.2"

resolvers += "Mesosphere Repo" at "http://downloads.mesosphere.io/maven"

libraryDependencies ++= {
  val akkaVersion = "2.3.6"
  val cassandraVersion = "2.1.0"
  val chaosVersion = "0.6.3"
  val commonsMath3Version = "3.2"
  val curatorFrameworkVersion = "2.6.0"
  val guavaVersion = "16.0.1"
  val jgraphtVersion = "0.9.1"
  val jodaConvertVersion = "1.7"
  val jodaTimeVersion = "2.3"
  val lz4Version = "1.2.0"
  val mesosUtilsVersion = "0.23.0"
  val metricsVersion = "3.1.0"
  val ravenVersion = "4.1.2"

  val akkaTestkitVersion = "2.3.6"
  val junitVersion = "4.11"
  val mockitoVersion = "1.9.5"
  val specs2Version = "3.6.4-20150927082714-7cac887"

  Seq(
    "com.typesafe.akka" % "akka-testkit_2.11" % akkaTestkitVersion % "test",
    "junit" % "junit" % junitVersion % "test",
    "org.mockito" % "mockito-all" % mockitoVersion % "test",
    "org.specs2" % "specs2-core_2.11" % specs2Version % "test",
    "org.specs2" % "specs2-mock_2.11" % specs2Version % "test",
    "org.specs2" % "specs2-junit_2.11" % specs2Version % "test",
    "org.apache.curator" % "curator-test" % curatorFrameworkVersion % "test",
    "mesosphere" % "chaos_2.11" % chaosVersion,
    "mesosphere" % "mesos-utils_2.11" % mesosUtilsVersion,
    "com.google.guava" % "guava" % guavaVersion,
    "joda-time" % "joda-time" % jodaTimeVersion,
    "org.joda" % "joda-convert" % jodaConvertVersion,
    "org.javabits.jgrapht" % "jgrapht-core" % jgraphtVersion,
    "org.javabits.jgrapht" % "jgrapht-ext" % jgraphtVersion,
    "org.apache.curator" % "curator-framework" % curatorFrameworkVersion,
    "org.apache.curator" % "curator-recipes" % curatorFrameworkVersion,
    "io.dropwizard.metrics" % "metrics-graphite" % metricsVersion,
    "com.datastax.cassandra" % "cassandra-driver-core" % cassandraVersion,
    "net.jpountz.lz4" % "lz4" % lz4Version,
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
    "net.kencochrane.raven" % "raven" % ravenVersion,
    "net.kencochrane.raven" % "raven-getsentry" % ravenVersion,
    "org.apache.commons" % "commons-email" % "1.3.2",
    "org.apache.commons" % "commons-math3" % commonsMath3Version,
    "commons-codec" % "commons-codec" % "1.10"
  )
}

// Maven
publishMavenStyle := true

