package com.airbnb.scheduler.jobs

import com.google.common.util.concurrent.AbstractIdleService
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils

class ZookeeperService(curator: CuratorFramework)
        extends AbstractIdleService
{
  override def startUp() {}
  override def shutDown() {
    CloseableUtils.closeQuietly(curator)
  }
}
