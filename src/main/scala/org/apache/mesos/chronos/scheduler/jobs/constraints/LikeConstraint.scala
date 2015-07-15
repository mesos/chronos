package org.apache.mesos.chronos.scheduler.jobs.constraints

import java.util.logging.Logger

import org.apache.mesos.Protos
import scala.collection.JavaConversions._

case class LikeConstraint(attribute: String, value: String) extends Constraint {

  val regex = value.r

  private[this] val log = Logger.getLogger(getClass.getName)

  def matches(attributes: Seq[Protos.Attribute]): Boolean = {
    attributes.find(a => a.getName == attribute).exists { a =>
      a.getType match {
        case Protos.Value.Type.SCALAR =>
          regex.pattern.matcher(a.getScalar.getValue.toString).matches()
        case Protos.Value.Type.SET =>
          a.getSet.getItemList.exists(regex.pattern.matcher(_).matches())
        case Protos.Value.Type.TEXT =>
          regex.pattern.matcher(a.getText.getValue).matches()
        case Protos.Value.Type.RANGES =>
          log.warning("Like constraint does not support attributes of type RANGES")
          false
        case _ =>
          val t = a.getType.getNumber
          log.warning(s"Unknown constraint with number $t in Like constraint")
          false
      }
    }
  }
}

object LikeConstraint {
  val OPERATOR = "LIKE"
}
