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
package org.apache.mesos.chronos.scheduler.mesos

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.codahale.metrics.{ Counter, MetricRegistry }
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
        mock[Counter])

      val testProbe = TestProbe()
      val mesosOfferReviverDelegate = new MesosOfferReviverDelegate(testProbe.ref, registry)

      mesosOfferReviverDelegate.reviveOffers()

      there was one(reviveOffersRequestCounter).inc()
    }
  }
}
