package org.apache.mesos.chronos.scheduler.mesos

import akka.actor.ActorRef
import com.codahale.metrics.MetricRegistry

object MesosOfferReviverDelegate {
  case object ReviveOffers
}

class MesosOfferReviverDelegate(offerReviverRef: ActorRef, registry: MetricRegistry) extends MesosOfferReviver {
  val reviveOffersRequestCounter = registry.counter(
    MetricRegistry.name(classOf[MesosOfferReviver], "reviveOffersRequestCount"))

  override def reviveOffers(): Unit = {
    reviveOffersRequestCounter.inc()
    offerReviverRef ! MesosOfferReviverDelegate.ReviveOffers
  }
}
