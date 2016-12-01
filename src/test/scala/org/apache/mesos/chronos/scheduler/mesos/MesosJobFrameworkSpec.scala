package org.apache.mesos.chronos.scheduler.mesos

import mesosphere.mesos.protos._
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.Protos.Offer
import org.apache.mesos.chronos.ChronosTestHelper._
import org.apache.mesos.chronos.scheduler.jobs.{BaseJob, JobScheduler, MockJobUtils, TaskManager}
import org.apache.mesos.{Protos, SchedulerDriver}
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
        mock[TaskManager],
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
