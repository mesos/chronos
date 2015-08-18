package org.apache.mesos.chronos.scheduler.mesos

import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.Scheduler
import org.apache.mesos.chronos.ChronosTestHelper._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

class MesosDriverFactorySpec extends SpecificationWithJUnit with Mockito {
  "MesosDriverFactorySpec" should {
    "always fetch the frameworkId from the state store before creating a driver" in {
      val scheduler: Scheduler = mock[Scheduler]
      val frameworkIdUtil: FrameworkIdUtil = mock[FrameworkIdUtil]
      val mesosDriverFactory = new MesosDriverFactory(
        scheduler,
        frameworkIdUtil,
        makeConfig(),
        mock[SchedulerDriverBuilder])

      frameworkIdUtil.fetch(any, any).returns(None)

      mesosDriverFactory.get()

      there was one(frameworkIdUtil).fetch(any, any)
      ok
    }
  }
}
