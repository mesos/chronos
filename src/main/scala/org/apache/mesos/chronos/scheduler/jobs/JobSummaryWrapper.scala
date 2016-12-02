package org.apache.mesos.chronos.scheduler.jobs

class JobSummary(
                  val name: String,
                  val status: String,
                  val state: String,
                  val schedule: String,
                  val parents: List[String],
                  val disabled: Boolean
                ) {
}

class JobSummaryWrapper(
                         val jobs: List[JobSummary]
                       ) {
}
