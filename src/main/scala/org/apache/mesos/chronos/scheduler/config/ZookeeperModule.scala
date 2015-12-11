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
package org.apache.mesos.chronos.scheduler.config

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.google.inject.{ AbstractModule, Inject, Provides, Singleton }
import mesosphere.chaos.http.HttpConf
import mesosphere.mesos.util.FrameworkIdUtil
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.{ CuratorFramework, CuratorFrameworkFactory }
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.utils.EnsurePath
import org.apache.mesos.chronos.scheduler.jobs.ZookeeperService
import org.apache.mesos.chronos.scheduler.state.{ MesosStatePersistenceStore, PersistenceStore }
import org.apache.mesos.state.{ State, ZooKeeperState }

/**
 * Guice glue-code for zookeeper related things.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Consider using Sindi or Subcut for DI.
class ZookeeperModule(val config: SchedulerConfiguration with HttpConf)
    extends AbstractModule {
  private val log = Logger.getLogger(getClass.getName)

  def configure() {}

  @Inject
  @Singleton
  @Provides
  def provideZookeeperClient(): CuratorFramework = {
    val curator = CuratorFrameworkFactory.builder()
      .connectionTimeoutMs(config.zooKeeperTimeout().toInt)
      .canBeReadOnly(false)
      .connectString(validateZkServers())
      .retryPolicy(new ExponentialBackoffRetry(1000, 10))
      .build()

    curator.start()
    log.info("Connecting to ZK...")
    curator.blockUntilConnected()
    curator
  }

  private def validateZkServers(): String = {
    val servers: Array[String] = config.zookeeperServers().split(",")
    servers.foreach({
      server =>
        require(server.split(":").size == 2, "Error, zookeeper servers must be provided in the form host1:port2,host2:port2")
    })
    servers.mkString(",")
  }

  @Inject
  @Singleton
  @Provides
  def provideZookeeperService(curator: CuratorFramework): ZookeeperService = {
    new ZookeeperService(curator)
  }

  @Inject
  @Singleton
  @Provides
  def provideState(): State = {
    new ZooKeeperState(config.zookeeperServers(),
      config.zooKeeperTimeout(),
      TimeUnit.MILLISECONDS,
      config.zooKeeperStatePath)
  }

  @Inject
  @Singleton
  @Provides
  def provideStore(zk: CuratorFramework, state: State): PersistenceStore = {
    val ensurePath: EnsurePath = new EnsurePath(config.zooKeeperStatePath)
    ensurePath.ensure(zk.getZookeeperClient)

    new MesosStatePersistenceStore(zk, config, state)
  }

  @Provides
  @Singleton
  def provideFrameworkIdUtil(state: State): FrameworkIdUtil = {
    new FrameworkIdUtil(state)
  }

  @Inject
  @Singleton
  @Provides
  def provideLeaderLatch(curator: CuratorFramework): LeaderLatch = {
    val ensurePath: EnsurePath = new EnsurePath(config.zooKeeperCandidatePath)
    ensurePath.ensure(curator.getZookeeperClient)

    val id = "%s:%d".format(config.hostname(), config.httpPort())
    new LeaderLatch(curator, config.zooKeeperCandidatePath, id)
  }
}
