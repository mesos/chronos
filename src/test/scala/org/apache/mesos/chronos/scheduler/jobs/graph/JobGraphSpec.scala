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
package org.apache.mesos.chronos.scheduler.jobs.graph

import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs.DependencyBasedJob
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException
import org.specs2.mock._
import org.specs2.mutable._

class JobGraphSpec extends SpecificationWithJUnit with Mockito {

  "JobGraph" should {
    "Adding a parent and child to the graph works as expected" in {
      val a = new DependencyBasedJob(Set(), "A", "noop")
      val b = new DependencyBasedJob(Set(), "B", "noop")
      val g = new JobGraph()
      g.addVertex(a)
      g.addVertex(b)
      g.addDependency(a.name, b.name)
      g.getChildren(a.name) must contain(b.name)
    }

    "Adding a circular dependency should not be allowed" in {
      val a = new DependencyBasedJob(Set(), "A", "noop")
      val b = new DependencyBasedJob(Set(), "B", "noop")
      val c = new DependencyBasedJob(Set(), "C", "noop")
      val g = new JobGraph()
      g.addVertex(a)
      g.addVertex(b)
      g.addVertex(c)
      g.addDependency(a.name, b.name)
      g.addDependency(b.name, c.name)
      g.addDependency(c.name, a.name) must throwA[CycleFoundException]
    }

    "Adding nodes with the same name should not be allowed" in {
      val a = new DependencyBasedJob(Set(), "A", "noop")
      val b = new DependencyBasedJob(Set(), "A", "noop")
      val g = new JobGraph()
      g.addVertex(a)
      g.addVertex(b) must throwA[Exception]
    }

    "Adding the same edge twice is idempotent" in {
      val a = new DependencyBasedJob(Set(), "A", "noop")
      val b = new DependencyBasedJob(Set(), "A", "noop")
      val g = new JobGraph()
      g.addVertex(a)
      g.addVertex(b) must throwA[Exception]
    }

    "Adding dependencies should create proper edges" in {
      val a = new DependencyBasedJob(Set(), "A", "noop")
      val b = new DependencyBasedJob(Set(), "B", "noop")
      val c = new DependencyBasedJob(Set(), "C", "noop")
      val d = new DependencyBasedJob(Set(), "D", "noop")
      val g = new JobGraph()
      g.addVertex(a)
      g.addVertex(b)
      g.addVertex(c)
      g.addVertex(d)
      g.addDependency(a.name, b.name)
      g.addDependency(b.name, d.name)
      g.addDependency(c.name, d.name)
      g.getEdgesToParents(d.name).toSet.size must_== 2
    }

    "A complex graph should be traversable in correct order" in {
      /**
       * A -> B -> D
       * \
       * \-> E
       * /
       * C --
       */
      val a = new DependencyBasedJob(Set(), "A", "noop")
      val b = new DependencyBasedJob(Set(), "B", "noop")
      val c = new DependencyBasedJob(Set(), "C", "noop")
      val d = new DependencyBasedJob(Set(), "D", "noop")
      val e = new DependencyBasedJob(Set(), "E", "noop")
      val graph = new JobGraph()
      graph.addVertex(a)
      graph.addVertex(b)
      graph.addVertex(c)
      graph.addVertex(d)
      graph.addVertex(e)
      graph.addDependency(a.name, b.name)
      graph.addDependency(a.name, c.name)
      graph.addDependency(b.name, d.name)
      graph.addDependency(b.name, e.name)
      graph.addDependency(c.name, e.name)
      val aCompleted = graph.getExecutableChildren(a.name)
      aCompleted.toSet must_== Set(b.name, c.name)

      val bCompleted = graph.getExecutableChildren(b.name)
      bCompleted.toSet must_== Set(d.name)

      val cCompleted = graph.getExecutableChildren(c.name)
      cCompleted.toSet must_== Set(e.name)

      val aCompleted2 = graph.getExecutableChildren(a.name)
      aCompleted2.toSet must_== Set(b.name, c.name)

      val cCompleted2 = graph.getExecutableChildren(c.name)
      cCompleted2.toSet must_== Set()

      val bCompleted2 = graph.getExecutableChildren(b.name)
      bCompleted2.toSet must_== Set(d.name, e.name)
    }

    "Replacing a vertex works" in {
      val a = new DependencyBasedJob(Set(), "A", "noopA")
      val b = new DependencyBasedJob(Set(), "B", "noopB")
      val c = new DependencyBasedJob(Set(), "C", "noopC")
      val d = new DependencyBasedJob(Set(), "C", "noopD")

      val graph = new JobGraph()
      graph.addVertex(a)
      graph.addVertex(b)
      graph.addVertex(c)

      graph.addDependency(a.name, b.name)
      graph.addDependency(a.name, c.name)
      graph.addDependency(b.name, c.name)
      graph.getChildren(a.name).map(x => graph.lookupVertex(x).get.command).toSet must_== Set("noopB", "noopC")
      graph.replaceVertex(c, d)

      graph.getChildren(a.name).toSet must_== Set(b.name, d.name)
      graph.getChildren(a.name).map(x => graph.lookupVertex(x).get.command).toSet must_== Set("noopB", "noopD")
      graph.getChildren(b.name).toSet must_== Set(d.name)
    }
  }

}
