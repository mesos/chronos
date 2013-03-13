package com.airbnb.scheduler

import com.yammer.metrics.core.HealthCheck
import com.yammer.metrics.core.HealthCheck.Result
import com.google.inject.Inject
import jobs.JobScheduler

/**
 * @author Florian Leibert (flo@leibert.de)
 */
class SchedulerHealthCheck @Inject()(val jobScheduler: JobScheduler) extends HealthCheck("scheduler-health") {

  //TODO(FL): Implement
  override def check(): Result = {
    return Result.healthy
  }
}
