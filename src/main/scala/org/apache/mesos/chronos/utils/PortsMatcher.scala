package org.apache.mesos.chronos.utils

import java.util.logging.Logger
import mesosphere.mesos.protos
import mesosphere.mesos.protos.{RangesResource, Resource}
import org.apache.mesos.Protos.Offer
import org.apache.mesos.chronos.scheduler.jobs.{PortMappings, BaseJob}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Random

/**
  * Utility class for checking if the ports resource in an offer matches the requirements of an app.
  */
class PortsMatcher(
  job: BaseJob,
  offer: Offer,
  acceptedResourceRoles: Set[String] = Set("*"),
  random: Random = Random) {

  import PortsMatcher._

  private[this] val log = Logger.getLogger(getClass.getName)

  /**
    * The resulting port matches which should be consumed from the offer. If no matching port ranges could
    * be generated from the offer, return `None`.
    */
  lazy val portRanges: Option[Seq[RangesResource]] = portsWithRoles.map(PortWithRole.createPortsResources)

  /**
    * @return true if and only if the port requirements could be fulfilled by the given offer.
    */
  def matches: Boolean = portRanges.isDefined

  /**
    * @return the resulting assigned (host) ports.
    */
  def ports: Seq[Long] = for {
    resource <- portRanges.getOrElse(Nil)
    range <- resource.ranges
    port <- range.asScala()
  } yield port

  private[this] def portsWithRoles: Option[Seq[PortWithRole]] = {

    val portMappings: Option[Seq[PortMappings]] = if (job.container != null)
      for {
        pms <- job.container.portMappings if pms.nonEmpty
      } yield pms
    else None

    (job.ports, portMappings) match {
      case (Nil,None) => // optimization for empty special case
        Some(Seq.empty)

      case (appPortSpec, Some(mappings)) =>
        // We use the mappings from the containers if they are available and ignore any other port specification.
        // We cannot warn about this because we autofill the ports field.
        mappedPortRanges(mappings)

      case (appPorts, None) if job.requirePorts =>
        findPortsInOffer(appPorts, failLog = true)

      case (appPorts, None) =>
        randomPorts(appPorts.size)
    }
  }

  /**
    * Try to find supplied ports in offer. Returns `None` if not all ports were found.
    */
  private[this] def findPortsInOffer(requiredPorts: Seq[Int], failLog: Boolean): Option[Seq[PortWithRole]] = {
    takeEnoughPortsOrNone(expectedSize = requiredPorts.size) {
      requiredPorts.iterator.map { (port: Int) =>
        offeredPortRanges.find(_.contains(port)).map { offeredRange =>
          PortWithRole(offeredRange.role, port)
        } orElse {
          if (failLog)
            log.info(
              s"Offer [${offer.getId.getValue}]. Couldn't find host port $port (of ${requiredPorts.mkString(", ")}) " +
                s"in any offered range for app [${job.name}]")
          None
        }
      }
    }
  }

  /**
    * Choose random ports from offer.
    */
  private[this] def randomPorts(numberOfPorts: Int): Option[Seq[PortWithRole]] = {
    takeEnoughPortsOrNone(expectedSize = numberOfPorts) {
      shuffledAvailablePorts.map(Some(_))
    } orElse {
      log.info(s"Offer [${offer.getId.getValue}]. Couldn't find $numberOfPorts ports in offer for app [${job.name}]")
      None
    }
  }

  /**
    * Try to find all non-zero host ports in offer and use random ports from the offer for dynamic host ports (=0).
    * Return `None` if not all host ports could be assigned this way.
    */
  private[this] def mappedPortRanges(mappings: Seq[PortMappings]): Option[Seq[PortWithRole]] = {
    takeEnoughPortsOrNone(expectedSize = mappings.size) {
      // non-dynamic hostPorts from port mappings
      val hostPortsFromMappings: Set[Int] = mappings.iterator.map(_.hostPort).filter(_ != 0).toSet
      // available ports without the ports that have been preset in the port mappings
      val availablePortsWithoutStaticHostPorts: Iterator[PortWithRole] =
        shuffledAvailablePorts.filter(portWithRole => !hostPortsFromMappings(portWithRole.port))

      mappings.iterator.map {
        case PortMappings(containerPort,hostPort, protocol) if hostPort == 0 =>
          if (!availablePortsWithoutStaticHostPorts.hasNext) {
            log.info(s"Offer [${offer.getId.getValue}]. Insufficient ports in offer for app [${job.name}]")
            None
          }
          else {
            val port = availablePortsWithoutStaticHostPorts.next();
            log.info(s"Offer [${offer.getId.getValue}]. ports [${port}] in offer for app [${job.name}]")
            Option(port)
          }
        case pm: PortMappings =>
          offeredPortRanges.find(_.contains(pm.hostPort)) match {
            case Some(PortRange(role, _, _)) =>
              Some(PortWithRole(role, pm.hostPort))
            case None =>
              log.info(s"Offer [${offer.getId.getValue}]. " +
                s"Cannot find range with host port ${pm.hostPort} for app [${job.name}]")
              None
          }
      }
    }
  }

  /**
    * Takes `expectedSize` ports from the given iterator if possible. Stops when encountering the first `None` port.
    */
  private[this] def takeEnoughPortsOrNone[T](expectedSize: Int)(ports: Iterator[Option[T]]): Option[Seq[T]] = {
    val allocatedPorts = ports.takeWhile(_.isDefined).take(expectedSize).flatten.toVector
    if (allocatedPorts.size == expectedSize) Some(allocatedPorts) else None
  }

  private[this] lazy val offeredPortRanges: Seq[PortRange] = {
    val portRangeIter = for {
      resource <- offer.getResourcesList.asScala.iterator
      if acceptedResourceRoles(resource.getRole) && resource.getName == Resource.PORTS
      rangeInResource <- resource.getRanges.getRangeList.asScala
    } yield PortRange(resource.getRole, rangeInResource.getBegin.toInt, rangeInResource.getEnd.toInt)
    portRangeIter.to[Seq]
  }

  private[this] def shuffledAvailablePorts: Iterator[PortWithRole] =
    PortWithRole.lazyRandomPortsFromRanges(random)(offeredPortRanges)
}

object PortsMatcher {

  case class PortWithRole(role: String, port: Int) {
    def toRange: protos.Range = {
      protos.Range(port.toLong, port.toLong)
    }
  }

  object PortWithRole {
    /**
      * Return RangesResources covering all given ports with the given roles.
      *
      * Creates as few RangesResources as possible while
      * preserving the order of the ports.
      */
    def createPortsResources(resources: Seq[PortWithRole]): Seq[RangesResource] = {
      /*
       * Create as few ranges as possible from the given ports while preserving the order of the ports.
       *
       * It does not check if the given ports have different roles.
       */
      def createRanges(ranges: Seq[PortWithRole]): Seq[protos.Range] = {
        val builder = Seq.newBuilder[protos.Range]

        @tailrec
        def process(lastRangeOpt: Option[protos.Range], next: Seq[PortWithRole]): Unit = {
          (lastRangeOpt, next.headOption) match {
            case (None, _) =>
            case (Some(lastRange), None) =>
              builder += lastRange
            case (Some(lastRange), Some(nextPort)) if lastRange.end == nextPort.port - 1 =>
              process(Some(lastRange.copy(end = nextPort.port.toLong)), next.tail)
            case (Some(lastRange), Some(nextPort)) =>
              builder += lastRange
              process(Some(nextPort.toRange), next.tail)
          }
        }
        process(ranges.headOption.map(_.toRange), ranges.tail)

        builder.result()
      }

      val builder = Seq.newBuilder[RangesResource]
      @tailrec
      def process(resources: Seq[PortWithRole]): Unit = resources.headOption match {
        case None =>
        case Some(PortWithRole(role, _)) =>
          val portsForResource: Seq[PortWithRole] = resources.takeWhile(_.role == role)
          builder += RangesResource(name = Resource.PORTS, createRanges(portsForResource), role = role)
          process(resources.drop(portsForResource.size))
      }
      process(resources)

      builder.result()
    }

    /**
      * We want to make it less likely that we are reusing the same dynamic port for tasks of different apps.
      * This way we allow load balancers to reconfigure before reusing the same ports.
      *
      * Therefore we want to choose dynamic ports randomly from all the offered port ranges.
      * We want to use consecutive ports to avoid excessive range fragmentation.
      *
      * The implementation idea:
      *
      * * Randomize the order of the offered ranges.
      * * Now treat the ports contained in the ranges as one long sequence of ports.
      * * We randomly choose an index where we want to start assigning dynamic ports in that sequence. When
      *   we hit the last offered port with wrap around and start offering the ports at the beginning
      *   of the sequence up to (excluding) the port index we started at.
      */
    def lazyRandomPortsFromRanges(rand: Random = Random)(offeredPortRanges: Seq[PortRange]): Iterator[PortWithRole] = {
      val numberOfOfferedPorts = offeredPortRanges.map(_.size).sum

      if (numberOfOfferedPorts == 0) {
        //scalastyle:off return
        return Iterator.empty
        //scalastyle:on
      }

      def findStartPort(shuffled: Vector[PortRange], startPortIdx: Int): (Int, Int) = {
        var startPortIdxOfCurrentRange = 0
        val rangeIdx = shuffled.indexWhere {
          case range: PortRange if startPortIdxOfCurrentRange + range.size > startPortIdx =>
            true
          case range: PortRange =>
            startPortIdxOfCurrentRange += range.size
            false
        }

        (rangeIdx, startPortIdx - startPortIdxOfCurrentRange)
      }

      val shuffled = rand.shuffle(offeredPortRanges).toVector
      val startPortIdx = rand.nextInt(numberOfOfferedPorts)
      val (rangeIdx, portInRangeIdx) = findStartPort(shuffled, startPortIdx)
      val startRangeOrig = shuffled(rangeIdx)

      val startRange = startRangeOrig.withoutNPorts(portInRangeIdx)

      // These are created on demand if necessary
      def afterStartRange: Iterator[PortWithRole] =
        shuffled.slice(rangeIdx + 1, shuffled.length).iterator.flatMap(_.portsWithRolesIterator)
      def beforeStartRange: Iterator[PortWithRole] =
        shuffled.slice(0, rangeIdx).iterator.flatMap(_.portsWithRolesIterator)
      def endRange: Iterator[PortWithRole] = startRangeOrig.firstNPorts(portInRangeIdx)

      startRange ++ afterStartRange ++ beforeStartRange ++ endRange
    }
  }

  case class PortRange(role: String, minPort: Int, maxPort: Int) {
    private[this] def range: Range.Inclusive = Range.inclusive(minPort, maxPort)

    def size: Int = range.size
    /*
     * Attention! range exports _two_ contains methods, a generic inefficient one and an efficient one
     * that only gets used with Int (and not java.lang.Integer and similar)
     */
    def contains(port: Int): Boolean = range.contains(port)

    def portsWithRolesIterator: Iterator[PortWithRole] = range.iterator.map(PortWithRole(role, _))
    def firstNPorts(n: Int): Iterator[PortWithRole] = range.take(n).iterator.map(PortWithRole(role, _))
    def withoutNPorts(n: Int): Iterator[PortWithRole] = range.drop(n).iterator.map(PortWithRole(role, _))
  }
}
