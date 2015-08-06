package org.apache.mesos.chronos.scheduler.mesos

/**
  * Request offers from Mesos that we have already seen because we have new launching requirements.
  */
trait MesosOfferReviver {
  def reviveOffers(): Unit
}
