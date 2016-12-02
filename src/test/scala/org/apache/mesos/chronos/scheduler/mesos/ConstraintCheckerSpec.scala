package org.apache.mesos.chronos.scheduler.mesos

import mesosphere.mesos.protos.Implicits._
import mesosphere.mesos.protos._
import org.apache.mesos.Protos
import org.apache.mesos.chronos.scheduler.jobs.constraints.{ConstraintSpecHelper, EqualsConstraint, LikeConstraint}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

class ConstraintCheckerSpec extends SpecificationWithJUnit
  with Mockito
  with ConstraintSpecHelper {

  val offer = Protos.Offer.newBuilder()
    .setId(OfferID("1"))
    .setFrameworkId(FrameworkID("chronos"))
    .setSlaveId(SlaveID("slave-1"))
    .setHostname("slave.one.com")
    .addAttributes(createTextAttribute("rack", "rack-1"))
    .build()

  val offerWithHostname = Protos.Offer.newBuilder()
    .setId(OfferID("1"))
    .setFrameworkId(FrameworkID("chronos"))
    .setSlaveId(SlaveID("slave-1"))
    .setHostname("slave.one.com")
    .addAttributes(createTextAttribute("hostname", "slave.explicit.com"))
    .build()

  "check constraints" should {

    "be true when equal" in {
      val constraints = Seq(EqualsConstraint("rack", "rack-1"))
      ConstraintChecker.checkConstraints(offer, constraints) must beTrue
    }

    "be false when not equal" in {
      val constraints = Seq(EqualsConstraint("rack", "rack-2"))
      ConstraintChecker.checkConstraints(offer, constraints) must beFalse
    }

    "be true when like" in {
      val constraints = Seq(LikeConstraint("rack", "rack-[1-3]"))
      ConstraintChecker.checkConstraints(offer, constraints) must beTrue
    }

    "be false when not like" in {
      val constraints = Seq(LikeConstraint("rack", "rack-[2-3]"))
      ConstraintChecker.checkConstraints(offer, constraints) must beFalse
    }

    "be true when hostname equal" in {
      val constraints = Seq(EqualsConstraint("hostname", "slave.one.com"))
      ConstraintChecker.checkConstraints(offer, constraints) must beTrue
    }

    "be false when hostname not equal" in {
      val constraints = Seq(EqualsConstraint("hostname", "slave.two.com"))
      ConstraintChecker.checkConstraints(offer, constraints) must beFalse
    }

    "be false when hostname explicitly set to something else and not equal" in {
      val constraints = Seq(EqualsConstraint("hostname", "slave.one.com"))
      ConstraintChecker.checkConstraints(offerWithHostname, constraints) must beFalse
    }

    "be true when hostname explicitly set to something else and equal" in {
      val constraints = Seq(EqualsConstraint("hostname", "slave.explicit.com"))
      ConstraintChecker.checkConstraints(offerWithHostname, constraints) must beTrue
    }
  }
}
