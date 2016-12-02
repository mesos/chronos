package org.apache.mesos.chronos.scheduler.mesos

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import akka.event.LoggingReceive
import com.codahale.metrics.MetricRegistry
import org.apache.mesos.Protos.Status
import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.utils.Timestamp

import scala.concurrent.duration._

object MesosOfferReviverActor {
  final val NAME = "mesosOfferReviver"

  def props(config: SchedulerConfiguration, mesosDriverFactory: MesosDriverFactory, registry: MetricRegistry): Props = {
    Props(new MesosOfferReviverActor(config, mesosDriverFactory, registry))
  }
}

/**
  * Revive offers whenever interest is signaled but maximally every 5 seconds.
  */
class MesosOfferReviverActor(
                              config: SchedulerConfiguration,
                              mesosDriverFactory: MesosDriverFactory,
                              registry: MetricRegistry) extends Actor with ActorLogging {
  private[this] val reviveCounter = registry.counter(
    MetricRegistry.name(classOf[MesosOfferReviver], "reviveOffersCount"))
  private[this] var lastRevive: Timestamp = Timestamp(0)
  private[this] var nextReviveCancellableOpt: Option[Cancellable] = None

  override def receive: Receive = LoggingReceive {
    case MesosOfferReviverDelegate.ReviveOffers =>
      log.info("Received request to revive offers")
      reviveOffers()
  }

  private[this] def reviveOffers(): Unit = {
    val now: Timestamp = Timestamp.now()
    val nextRevive: Timestamp = lastRevive + config.minReviveOffersInterval().milliseconds

    if (nextRevive <= now) {
      log.info("=> reviving offers NOW, canceling any scheduled revives")
      nextReviveCancellableOpt.foreach(_.cancel())
      nextReviveCancellableOpt = None

      if (mesosDriverFactory.get.reviveOffers() != Status.DRIVER_RUNNING) {
        throw new RuntimeException("Driver is no longer running")
      }
      lastRevive = now

      reviveCounter.inc()
    }
    else {
      lazy val untilNextRevive = now until nextRevive
      if (nextReviveCancellableOpt.isEmpty) {
        log.info("=> Scheduling next revive at {} in {}, adhering to --{} {} (ms)",
          nextRevive, untilNextRevive, config.minReviveOffersInterval.name, config.minReviveOffersInterval())
        nextReviveCancellableOpt = Some(scheduleCheck(untilNextRevive))
      }
      else {
        log.info("=> Next revive already scheduled at {}, due in {}", nextRevive, untilNextRevive)
      }
    }
  }

  protected def scheduleCheck(duration: FiniteDuration): Cancellable = {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(duration, self, MesosOfferReviverDelegate.ReviveOffers)
  }
}
