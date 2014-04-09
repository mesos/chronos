package com.airbnb.scheduler.jobs

import com.google.common.util.concurrent.AbstractIdleService
import org.apache.curator.framework.CuratorFramework

class ZookeeperService(curator: CuratorFramework)
        extends AbstractIdleService
{
  override def startUp() {
    curator.start()
  }

  override def shutDown() {
    curator.close()
  }
}
