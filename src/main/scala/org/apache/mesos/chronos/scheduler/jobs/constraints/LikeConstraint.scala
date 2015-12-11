/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
