package org.apache.mesos.chronos.scheduler.mesos

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.codahale.metrics.{Counter, MetricRegistry}
import org.specs2.mock._
import org.specs2.mutable._

import scala.concurrent.duration._

class MesosOfferReviverDelegateSpec extends SpecificationWithJUnit with Mockito {
  implicit lazy val system = ActorSystem()

  "MesosOfferReviverDelegate" should {
    "Send a ReviveOffers message " in {
      val registry = spy(new MetricRegistry())
      val testProbe = TestProbe()
      val mesosOfferReviverDelegate = new MesosOfferReviverDelegate(testProbe.ref, registry)

      mesosOfferReviverDelegate.reviveOffers()

      testProbe.expectMsg(FiniteDuration(500, TimeUnit.MILLISECONDS), MesosOfferReviverDelegate.ReviveOffers)

      ok
    }

    "Increment the ReviveOfferRequests counter" in {
      val registry = spy(new MetricRegistry())

      val reviveOffersRequestCounter = registry.register(
        MetricRegistry.name(classOf[MesosOfferReviver], "reviveOffersRequestCount"),
        mock[Counter]
      )

      val testProbe = TestProbe()
      val mesosOfferReviverDelegate = new MesosOfferReviverDelegate(testProbe.ref, registry)

      mesosOfferReviverDelegate.reviveOffers()

      there was one(reviveOffersRequestCounter).inc()
    }
  }
}
