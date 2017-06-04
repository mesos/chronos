package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger

import org.apache.mesos.Protos

/**
  * Helper for checking availability using mesos primitives
  */
object AvailabilityChecker {

  private[this] val log = Logger.getLogger(getClass.getName)

  def checkAvailability(offer: Protos.Offer): Boolean = {
    var unavailability = offer.hasUnavailability
    var now = System.nanoTime()
    if (offer.hasUnavailability && offer.getUnavailability.hasStart) {
      val start = offer.getUnavailability.getStart.getNanoseconds
      if (now.>=(start)) {
        if (offer.getUnavailability.hasDuration) {
          return start.+(offer.getUnavailability.getDuration.getNanoseconds).>(now)
        } else {
          return false;
        }

      }
    }
    return true
  }

}
