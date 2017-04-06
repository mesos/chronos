package org.apache.mesos.chronos.notification

sealed trait NotificationLevel extends Ordered[NotificationLevel] {
  def toInt: Int

  def compare(that: NotificationLevel) = this.toInt.compare(that.toInt)
}

object NotificationLevel {
  case object Disabled extends NotificationLevel { def toInt = 0 }
  case object Failures extends NotificationLevel { def toInt = 1 }
  case object All extends NotificationLevel { def toInt = 2 }

  def apply(s: String): Option[NotificationLevel] = s match {
    case "disabled" => Some(NotificationLevel.Disabled)
    case "failures" => Some(NotificationLevel.Failures)
    case "all" => Some(NotificationLevel.All)
    case _ => None
  }
}
