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

import java.util.regex.PatternSyntaxException

import org.specs2.mutable.SpecificationWithJUnit

class LikeConstraintSpec extends SpecificationWithJUnit
    with ConstraintSpecHelper {
  "matches attributes of type text" in {
    val attributes = List(createTextAttribute("dc", "north"), createTextAttribute("rack", "rack-1"))
    val constraint = LikeConstraint("rack", "rack-[1-3]")

    constraint.matches(attributes) must_== true

    val attributes2 = List(createTextAttribute("dc", "north"))
    val constraint2 = LikeConstraint("dc", "north|south")

    constraint2.matches(attributes2) must_== true
  }

  "matches attributes of type scalar" in {
    val attributes = List(createScalarAttribute("number", 1))
    val constraint = LikeConstraint("number", """\d\.\d""")

    constraint.matches(attributes) must_== true
  }

  "matches attributes of type set" in {
    val attributes = List(createSetAttribute("dc", Array("north")))
    val constraint = LikeConstraint("dc", "^n.*")

    constraint.matches(attributes) must_== true

    val attributes2 = List(createSetAttribute("dc", Array("south")))

    constraint.matches(attributes2) must_== false
  }

  "does not match attributes" in {
    val attributes = List(createTextAttribute("dc", "north"), createTextAttribute("rack", "rack-1"))

    val constraint = LikeConstraint("rack", "rack-[2-3]")

    constraint.matches(attributes) must_== false
  }

  "fails in case of an invalid regular expression" in {
    LikeConstraint("invalid-regex", "[[[") must throwA[PatternSyntaxException]
  }
}
