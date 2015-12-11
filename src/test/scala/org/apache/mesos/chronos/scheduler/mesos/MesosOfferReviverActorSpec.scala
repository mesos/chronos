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

import akka.actor._
import akka.testkit.TestProbe
import com.codahale.metrics.{ Counter, MetricRegistry }
import mesosphere.chaos.http.HttpConf
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.SchedulerDriver
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.specs2.mock.Mockito
import org.specs2.mutable._

import scala.concurrent.duration.FiniteDuration

class MesosOfferReviverActorSpec extends SpecificationWithJUnit with Mockito {
  "MesosOfferReviverActor" should {
    "Call the driver's reviveOffers on ReviveOffers message" in new context {
      val actorRef = startActor()

      delegate.reviveOffers()

      // wait for the actor to process all messages and die
      actorRef ! PoisonPill
      val probe = TestProbe()
      probe.watch(actorRef)
      probe.expectMsgAnyClassOf(classOf[Terminated])

      there was one(driverFactory.get).reviveOffers()
    }

    "increment the reviveOffersCounter when it revives offers" in new context {
      val actorRef = startActor()

      delegate.reviveOffers()

      // wait for the actor to process all messages and die
      actorRef ! PoisonPill
      val probe = TestProbe()
      probe.watch(actorRef)
      probe.expectMsgAnyClassOf(classOf[Terminated])

      actorSystem.shutdown()
      actorSystem.awaitTermination()

      there was one(reviveOffersCounter).inc()
    }

    "Second revive offers message results in scheduling a call to the driver's reviveOffers method" in new context {
      @volatile
      var scheduleCheckCalled = 0

      val actorRef = startActor(Props(
        new MesosOfferReviverActor(conf, driverFactory, metrics) {
          override protected def scheduleCheck(duration: FiniteDuration): Cancellable = {
            scheduleCheckCalled += 1
            mock[Cancellable]
          }
        }))

      delegate.reviveOffers()
      delegate.reviveOffers()

      // wait for the actor to process all messages and die
      actorRef ! PoisonPill
      val probe = TestProbe()
      probe.watch(actorRef)
      probe.expectMsgAnyClassOf(classOf[Terminated])

      there was one(driverFactory.mesosDriver.get).reviveOffers()
      there was one(reviveOffersCounter).inc()

      scheduleCheckCalled must beEqualTo(1)
    }
  }
}

trait context extends BeforeAfter with Mockito {
  implicit var actorSystem: ActorSystem = _
  var driverFactory: MesosDriverFactory = _
  var conf: SchedulerConfiguration with HttpConf = _
  var delegate: MesosOfferReviverDelegate = _
  var metrics: MetricRegistry = _
  var reviveOffersCounter: Counter = _

  def before = {
    actorSystem = ActorSystem()
    conf = new SchedulerConfiguration with HttpConf {}
    conf.afterInit()
    driverFactory = new MesosDriverFactory(mock[org.apache.mesos.Scheduler], mock[FrameworkIdUtil], conf)
    driverFactory.mesosDriver = Some(mock[SchedulerDriver])

    metrics = spy(new MetricRegistry())
    reviveOffersCounter = metrics.register(
      MetricRegistry.name(classOf[MesosOfferReviver], "reviveOffersCount"),
      mock[Counter])
  }

  def after = {
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }

  def startActor(props: Props = MesosOfferReviverActor.props(conf, driverFactory, metrics)): ActorRef = {
    val actorRef = actorSystem.actorOf(props, MesosOfferReviverActor.NAME)
    delegate = new MesosOfferReviverDelegate(actorRef, metrics)

    actorRef
  }
}
