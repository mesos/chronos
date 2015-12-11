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

import org.specs2.mutable.SpecificationWithJUnit

class EqualsConstraintSpec extends SpecificationWithJUnit
    with ConstraintSpecHelper {

  "matches attributes" in {
    val attributes = List(createTextAttribute("dc", "north"), createTextAttribute("rack", "rack-1"))

    val constraint = EqualsConstraint("rack", "rack-1")

    constraint.matches(attributes) must_== true
  }

  "does not match attributes" in {
    val attributes = List(createTextAttribute("dc", "north"), createTextAttribute("rack", "rack-1"))

    val constraint = EqualsConstraint("rack", "rack-2")

    constraint.matches(attributes) must_== false
  }

}
