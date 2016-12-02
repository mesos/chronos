package org.apache.mesos.chronos.scheduler.mesos

import java.util.logging.Logger

import org.apache.mesos.Protos
import org.apache.mesos.chronos.scheduler.jobs.constraints.Constraint

import scala.collection.JavaConverters._

/**
  * Helper for checking resource offer against job constraints
  */
object ConstraintChecker {
  val Hostname = "hostname"
  private[this] val log = Logger.getLogger(getClass.getName)

  def checkConstraints(offer: Protos.Offer, constraints: Seq[Constraint]): Boolean = {
    var attributes = offer.getAttributesList.asScala

    if (!attributes.exists(attr => attr.getName == Hostname)) {
      log.fine(s"adding hostname-attribute=${offer.getHostname} to offer=${offer}")
      val hostnameText = Protos.Value.Text.newBuilder().setValue(offer.getHostname).build()
      val hostnameAttribute = Protos.Attribute.newBuilder().setName(Hostname).setText(hostnameText).setType(Protos.Value.Type.TEXT).build()
      attributes = offer.getAttributesList.asScala :+ hostnameAttribute
    }

    constraints.forall(_.matches(attributes))
  }

}
