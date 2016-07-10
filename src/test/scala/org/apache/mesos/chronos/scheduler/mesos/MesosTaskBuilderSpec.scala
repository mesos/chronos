package org.apache.mesos.chronos.scheduler.mesos

import scala.collection.JavaConversions._
import org.apache.mesos.Protos._
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.jobs.Parameter
import org.apache.mesos.chronos.scheduler.jobs.Volume
import org.apache.mesos.chronos.scheduler.jobs._
import org.apache.mesos.chronos.scheduler.jobs.constraints.{LikeConstraint, EqualsConstraint}
import org.joda.time.Minutes
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit


class MesosTaskBuilderSpec extends SpecificationWithJUnit with Mockito {

  val taskId = "ct:1454467003926:0:test2Execution:run"

  val (_, start, attempt, _) = TaskUtils.parseTaskId(taskId)

  val offer = Offer.newBuilder().mergeFrom(Offer.getDefaultInstance)
    .setHostname("localport")
    .setId(OfferID.newBuilder().setValue("123").build())
    .setFrameworkId(FrameworkID.newBuilder().setValue("123").build())
    .setSlaveId(SlaveID.newBuilder().setValue("123").build())
    .build()

  val job = {
    val volumes = Seq(
      Volume(Option("/host/dir"), "container/dir", Option(VolumeMode.RW), None, None),
      Volume(None, "container/dir", None, None, None)
    )

    val parameters = scala.collection.mutable.ListBuffer[Parameter]()

    val container = Container("dockerImage", ContainerType.DOCKER, volumes, parameters, NetworkMode.HOST, None, true)

    val constraints = Seq(
      EqualsConstraint("rack", "rack-1"),
      LikeConstraint("rack", "rack-[1-3]")
    )

    new ScheduleBasedJob("FOO/BAR/BAM", "AJob", "noop", Minutes.minutes(5).toPeriod, 10L, 20L,
      "fooexec", "fooflags", "none", 7, "foo@bar.com", "Foo", "Test schedule based job", "TODAY",
      "YESTERDAY", true, cpus = 2, disk = 3, mem = 5, container = container, environmentVariables = Seq(),
      shell = true, arguments = Seq(), softError = true, constraints = constraints)
  }

  val defaultEnv = Map(
    "mesos_task_id"           -> taskId,
    "CHRONOS_JOB_OWNER"       -> job.owner,
    "CHRONOS_JOB_NAME"        -> job.name,
    "HOST"                    -> offer.getHostname,
    "CHRONOS_RESOURCE_MEM"    -> job.mem.toString,
    "CHRONOS_RESOURCE_CPU"    -> job.cpus.toString,
    "CHRONOS_RESOURCE_DISK"   -> job.disk.toString,
    "CHRONOS_JOB_RUN_TIME"    -> start.toString,
    "CHRONOS_JOB_RUN_ATTEMPT" -> attempt.toString
  )

  def toMap(envs: Environment): Map[String, String] =
    envs.getVariablesList.foldLeft(Map[String, String]())((m, v) => m + (v.getName -> v.getValue))

  "MesosTaskBuilder" should {
    "Setup all the default environment variables" in {
      val target = new MesosTaskBuilder(mock[SchedulerConfiguration])

      defaultEnv must_== toMap(target.envs(taskId, job, offer).build())
    }
  }

  "MesosTaskBuilder" should {
    "Setup all the default environment variables and job environment variables" in {
      val target = new MesosTaskBuilder(mock[SchedulerConfiguration])

      val testJob = job.copy(environmentVariables = Seq(
        EnvironmentVariable("FOO", "BAR"),
        EnvironmentVariable("TOM", "JERRY")
      ))

      val finalEnv = defaultEnv ++ Map("FOO" -> "BAR", "TOM" -> "JERRY")

      finalEnv must_== toMap(target.envs(taskId, testJob, offer).build())
    }
  }

  "MesosTaskBuilder" should {
    "Should not allow job environment variables to overwrite any default environment variables" in {
      val target = new MesosTaskBuilder(mock[SchedulerConfiguration])

      val testJob = job.copy(environmentVariables = Seq(
        EnvironmentVariable("CHRONOS_RESOURCE_MEM", "10000"),
        EnvironmentVariable("CHRONOS_RESOURCE_DISK", "40000")
      ))
      
      defaultEnv must_== toMap(target.envs(taskId, testJob, offer).build())
    }
  }
}
