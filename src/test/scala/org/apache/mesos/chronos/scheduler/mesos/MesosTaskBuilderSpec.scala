package org.apache.mesos.chronos.scheduler.mesos

import mesosphere.mesos.protos._
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.mesos.Protos.Offer
import org.apache.mesos.chronos.ChronosTestHelper._
import org.apache.mesos.chronos.scheduler.jobs._
import org.apache.mesos.{ Protos, SchedulerDriver }
import org.mockito.Mockito.{ doNothing, doReturn }
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit

import scala.collection.mutable

class MesosTaskBuilderSpec extends SpecificationWithJUnit {
  final val DEFAULT_ROLE_NAME = "*"
  final val RESERVED_ROLE_NAME = "reserved"

  "MesosTaskBuilder" should {
    "Use reserved role when single port matches reserved port ranges" in {
      val portMapping = PortMapping(10000, 65535)
      val builder = new MesosTaskBuilder(null)
      val container = DockerContainer("image", Seq[Volume](), portMappings = Seq(portMapping))
      val job = ScheduleBasedJob("R//P0", "Test", "/bin/true", container = container)
      val rc = builder.rangesResource(Resource.PORTS, container.portMappings.map(x => x.hostPort.toLong), makeOffer(RESERVED_ROLE_NAME))
      rc must not beNull; // The trailing ';' is required here
      rc.getName === Resource.PORTS
      rc.getRole === RESERVED_ROLE_NAME
      rc.getRanges must not beNull; // The trailing ';' is required here
      rc.getRanges.getRangeList must have size 1
      rc.getRanges.getRangeList.get(0).getBegin === portMapping.hostPort
      rc.getRanges.getRangeList.get(0).getEnd === portMapping.hostPort
    }

    "Use default role when single port does not match reserved port ranges" in {
      val portMapping = PortMapping(40000, 65535)
      val builder = new MesosTaskBuilder(null)
      val container = DockerContainer("image", Seq[Volume](), portMappings = Seq(portMapping))
      val job = ScheduleBasedJob("R//P0", "Test", "/bin/true", container = container)
      val rc = builder.rangesResource(Resource.PORTS, container.portMappings.map(x => x.hostPort.toLong), makeOffer(RESERVED_ROLE_NAME))
      rc must not beNull; // The trailing ';' is required here
      rc.getName === Resource.PORTS
      rc.getRole === DEFAULT_ROLE_NAME
      rc.getRanges must not beNull; // The trailing ';' is required here
      rc.getRanges.getRangeList must have size 1
      rc.getRanges.getRangeList.get(0).getBegin === portMapping.hostPort
      rc.getRanges.getRangeList.get(0).getEnd === portMapping.hostPort
    }

    "Use default role when single port matches default port ranges" in {
      val portMapping = PortMapping(10000, 65535)
      val builder = new MesosTaskBuilder(null)
      val container = DockerContainer("image", Seq[Volume](), portMappings = Seq(portMapping))
      val job = ScheduleBasedJob("R//P0", "Test", "/bin/true", container = container)
      val rc = builder.rangesResource(Resource.PORTS, container.portMappings.map(x => x.hostPort.toLong), makeOffer(DEFAULT_ROLE_NAME))
      rc must not beNull; // The trailing ';' is required here
      rc.getName === Resource.PORTS
      rc.getRole === DEFAULT_ROLE_NAME
      rc.getRanges must not beNull; // The trailing ';' is required here
      rc.getRanges.getRangeList must have size 1
      rc.getRanges.getRangeList.get(0).getBegin === portMapping.hostPort
      rc.getRanges.getRangeList.get(0).getEnd === portMapping.hostPort
    }

    "Use default role when single port does not match default port ranges" in {
      val portMapping = PortMapping(40000, 65535)
      val builder = new MesosTaskBuilder(null)
      val container = DockerContainer("image", Seq[Volume](), portMappings = Seq(portMapping))
      val job = ScheduleBasedJob("R//P0", "Test", "/bin/true", container = container)
      val rc = builder.rangesResource(Resource.PORTS, container.portMappings.map(x => x.hostPort.toLong), makeOffer(DEFAULT_ROLE_NAME))
      rc must not beNull; // The trailing ';' is required here
      rc.getName === Resource.PORTS
      rc.getRole === DEFAULT_ROLE_NAME
      rc.getRanges must not beNull; // The trailing ';' is required here
      rc.getRanges.getRangeList must have size 1
      rc.getRanges.getRangeList.get(0).getBegin === portMapping.hostPort
      rc.getRanges.getRangeList.get(0).getEnd === portMapping.hostPort
    }

    "Use reserved role when multiple ports match reserved port ranges" in {
      val portMappings = Seq(PortMapping(10000, 10000), PortMapping(11000, 11000), PortMapping(20000, 20000), PortMapping(21000, 21000), PortMapping(30000, 30000), PortMapping(31000, 31000))
      val builder = new MesosTaskBuilder(null)
      val container = DockerContainer("image", Seq[Volume](), portMappings = portMappings)
      val job = ScheduleBasedJob("R//P0", "Test", "/bin/true", container = container)
      val rc = builder.rangesResource(Resource.PORTS, container.portMappings.map(x => x.hostPort.toLong), makeOffer(RESERVED_ROLE_NAME))
      rc must not beNull; // The trailing ';' is required here
      rc.getName === Resource.PORTS
      rc.getRole === RESERVED_ROLE_NAME
      rc.getRanges must not beNull; // The trailing ';' is required here
      rc.getRanges.getRangeList must have size 6
      for (i <- 0 to 5) {
        rc.getRanges.getRangeList.get(i).getBegin === portMappings(i).hostPort
        rc.getRanges.getRangeList.get(i).getEnd === portMappings(i).hostPort
      }
      ok
    }

    "Use default role when one of multiple ports does not match reserved port ranges" in {
      val portMappings = Seq(PortMapping(10000, 10000), PortMapping(20000, 20000), PortMapping(30000, 30000), PortMapping(40000, 40000))
      val builder = new MesosTaskBuilder(null)
      val container = DockerContainer("image", Seq[Volume](), portMappings = portMappings)
      val job = ScheduleBasedJob("R//P0", "Test", "/bin/true", container = container)
      val rc = builder.rangesResource(Resource.PORTS, container.portMappings.map(x => x.hostPort.toLong), makeOffer(RESERVED_ROLE_NAME))
      rc must not beNull; // The trailing ';' is required here
      rc.getName === Resource.PORTS
      rc.getRole === DEFAULT_ROLE_NAME
      rc.getRanges must not beNull; // The trailing ';' is required here
      rc.getRanges.getRangeList must have size 4
      for (i <- 0 to 3) {
        rc.getRanges.getRangeList.get(i).getBegin === portMappings(i).hostPort
        rc.getRanges.getRangeList.get(i).getEnd === portMappings(i).hostPort
      }
      ok
    }
  }

  private[this] def makeOffer(roleName: String): Offer = {
    import mesosphere.mesos.protos.Implicits._

    Protos.Offer.newBuilder()
      .setId(OfferID("1"))
      .setFrameworkId(FrameworkID("chronos"))
      .setSlaveId(SlaveID("slave0"))
      .setHostname("localhost")
      .addResources(RangesResource(Resource.PORTS, Seq(Range(10000l, 11000l), Range(20000l, 21000l), Range(30000l, 31000l)), roleName))
      .build()
  }
}
