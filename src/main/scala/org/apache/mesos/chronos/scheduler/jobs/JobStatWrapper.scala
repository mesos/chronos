package org.apache.mesos.chronos.scheduler.jobs

import com.codahale.metrics.Histogram

class JobStatWrapper(val taskStats: List[TaskStat],
                     val hist: Histogram) {
}
