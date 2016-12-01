package org.apache.mesos.chronos.scheduler.mesos

import mesosphere.mesos.protos._
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.Protos.Offer
import org.apache.mesos.chronos.ChronosTestHelper._
import org.apache.mesos.chronos.scheduler.jobs.{BaseJob, JobScheduler, MockJobUtils, TaskManager}
import org.apache.mesos.{Protos, SchedulerDriver}
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

import scala.collection.mutable

class MesosJobFrameworkSpec extends SpecificationWithJUnit with Mockito {
  "MesosJobFramework" should {
    "Revive offers when registering" in {
      val mockMesosOfferReviver = mock[MesosOfferReviver]

      val mesosJobFramework = new MesosJobFramework(
        MockJobUtils.mockDriverFactory,
        mock[JobScheduler],
        mock[TaskManager],
        makeConfig(),
        mock[FrameworkIdUtil],
        mock[MesosTaskBuilder],
        mockMesosOfferReviver)

      mesosJobFramework.registered(mock[SchedulerDriver], Protos.FrameworkID.getDefaultInstance,
        Protos.MasterInfo.getDefaultInstance)

      there was one(mockMesosOfferReviver).reviveOffers()
    }

    "Revive offers when re-registering" in {
      val mockMesosOfferReviver = mock[MesosOfferReviver]

      val mesosJobFramework = new MesosJobFramework(
        mock[MesosDriverFactory],
        mock[JobScheduler],
        mock[TaskManager],
        makeConfig(),
        mock[FrameworkIdUtil],
        mock[MesosTaskBuilder],
        mockMesosOfferReviver)

      mesosJobFramework.reregistered(mock[SchedulerDriver], Protos.MasterInfo.getDefaultInstance)

      there was one(mockMesosOfferReviver).reviveOffers()
    }

    "Reject unused offers with the default " in {
      import mesosphere.mesos.protos.Implicits._
      import scala.collection.JavaConverters._

      val mockDriverFactory = MockJobUtils.mockDriverFactory
      val mockSchedulerDriver = mockDriverFactory.get

      val mesosJobFramework = spy(
        new MesosJobFramework(
          mockDriverFactory,
          mock[JobScheduler],
          mock[TaskManager],
          makeConfig(),
          mock[FrameworkIdUtil],
          mock[MesosTaskBuilder],
          mock[MesosOfferReviver]))

      val tasks = mutable.Buffer[(String, BaseJob, Offer)]()
      doReturn(tasks).when(mesosJobFramework).generateLaunchableTasks(any)
      doNothing.when(mesosJobFramework).reconcile(any)

      val offer: Offer = makeBasicOffer
      mesosJobFramework.resourceOffers(mockSchedulerDriver, Seq[Protos.Offer](offer).asJava)

      there was one (mockSchedulerDriver).declineOffer(OfferID("1"), Protos.Filters.getDefaultInstance)
    }

    "Reject unused offers with default RefuseSeconds if --decline_offer_duration is not set" in {
      import mesosphere.mesos.protos.Implicits._
      import scala.collection.JavaConverters._

      val mockDriverFactory = MockJobUtils.mockDriverFactory
      val mockSchedulerDriver = mockDriverFactory.get

      val mesosJobFramework = spy(
        new MesosJobFramework(
          mockDriverFactory,
          mock[JobScheduler],
          mock[TaskManager],
          makeConfig(),
          mock[FrameworkIdUtil],
          mock[MesosTaskBuilder],
          mock[MesosOfferReviver]))

      val tasks = mutable.Buffer[(String, BaseJob, Offer)]()
      doReturn(tasks).when(mesosJobFramework).generateLaunchableTasks(any)
      doNothing.when(mesosJobFramework).reconcile(any)

      val offer: Offer = makeBasicOffer
      mesosJobFramework.resourceOffers(mockSchedulerDriver, Seq[Protos.Offer](offer).asJava)

      there was one (mockSchedulerDriver).declineOffer(OfferID("1"), Protos.Filters.getDefaultInstance)
    }

    "Handle status updates without crashing" in {
      import mesosphere.mesos.protos.Implicits._

      val mockMesosOfferReviver = mock[MesosOfferReviver]
      val mockDriverFactory = MockJobUtils.mockDriverFactory
      val jobScheduler = mock[JobScheduler]

      val mesosJobFramework = new MesosJobFramework(
        mockDriverFactory,
        jobScheduler,
        MockJobUtils.mockTaskManager,
        makeConfig(),
        mock[FrameworkIdUtil],
        mock[MesosTaskBuilder],
        mockMesosOfferReviver)

      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:task1:"), TaskStaging))
      mesosJobFramework.getRunningCount("task1") must_== 1
      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:task1:"), TaskRunning))
      mesosJobFramework.getRunningCount("task1") must_== 1
      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:task1:"), TaskFinished))
      mesosJobFramework.getRunningCount("task1") must_== 0
      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:task1:"), TaskRunning))
      mesosJobFramework.getRunningCount("task1") must_== 1
      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:task1:"), TaskFailed))
      mesosJobFramework.getRunningCount("task1") must_== 0
      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:task1:"), TaskRunning))
      mesosJobFramework.getRunningCount("task1") must_== 1
      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:task1:"), TaskKilled))
      mesosJobFramework.getRunningCount("task1") must_== 0
      mesosJobFramework.statusUpdate(mockDriverFactory.get, TaskStatus(TaskID("ct:0:1:unknown:"), TaskFailed))
      mesosJobFramework.getRunningCount("unknown") must_== 0

      there was 4.times (jobScheduler).handleStartedTask(any[org.apache.mesos.Protos.TaskStatus], any[Int])
      there was one (jobScheduler).handleFinishedTask(any[org.apache.mesos.Protos.TaskStatus], any, any[Int])
      there was 2.times (jobScheduler).handleFailedTask(any[org.apache.mesos.Protos.TaskStatus], any[Int])
      there was one (jobScheduler).handleKilledTask(any[org.apache.mesos.Protos.TaskStatus], any[Int])
    }
  }

  "Reject unused offers with the configured value of RefuseSeconds if --decline_offer_duration is set" in {
    import mesosphere.mesos.protos.Implicits._

    import scala.collection.JavaConverters._

    val mesosDriverFactory = MockJobUtils.mockDriverFactory
    val mockSchedulerDriver = mesosDriverFactory.get

    val mesosJobFramework = spy(
      new MesosJobFramework(
        mesosDriverFactory,
        mock[JobScheduler],
        MockJobUtils.mockTaskManager,
        makeConfig("--decline_offer_duration", "3000"),
        mock[FrameworkIdUtil],
        mock[MesosTaskBuilder],
        mock[MesosOfferReviver]))

    val tasks = mutable.Buffer[(String, BaseJob, Offer)]()
    doReturn(tasks).when(mesosJobFramework).generateLaunchableTasks(any)
    doNothing.when(mesosJobFramework).reconcile(any)

    val offer: Offer = makeBasicOffer
    mesosJobFramework.resourceOffers(mockSchedulerDriver, Seq[Protos.Offer](offer).asJava)

    val filters = Protos.Filters.newBuilder().setRefuseSeconds(3).build()
    there was one (mockSchedulerDriver).declineOffer(OfferID("1"), filters)
  }

  private[this] def makeBasicOffer: Offer = {
    import mesosphere.mesos.protos.Implicits._

    Protos.Offer.newBuilder()
      .setId(OfferID("1"))
      .setFrameworkId(FrameworkID("chronos"))
      .setSlaveId(SlaveID("slave0"))
      .setHostname("localhost")
      .addResources(ScalarResource(Resource.CPUS, 1, "*"))
      .addResources(ScalarResource(Resource.MEM, 100, "*"))
      .addResources(ScalarResource(Resource.DISK, 100, "*"))
      .build()
  }


}
