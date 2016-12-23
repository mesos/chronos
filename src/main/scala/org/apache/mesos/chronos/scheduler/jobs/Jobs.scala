package org.apache.mesos.chronos.scheduler.jobs

import org.apache.mesos.chronos.scheduler.jobs.constraints.Constraint
import org.apache.mesos.chronos.utils.JobDeserializer
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.joda.time.{Minutes, Period}

/**
 * BaseJob encapsulates job specific information. BaseJob is defined for all tasks within a job.
 * At a bare minimum, it contains the command and a default epsilon value. Epsilon is the maximum allowed delay that a
 * job may be triggered at - if a job cannot be scheduled within epsilon (e.g. no resources),
 * the execution cycle is skipped.
 * @author Florian Leibert (flo@leibert.de)
 */
//The fact that Job is a trait rather than part of this class is a problem with dropwizards json serializer which will
//omit fields defined in superclasses but not traits.

// It may be surprising that a DependencyBasedJob (DPJ) has an epsilon: If it didn't have an epsilon, and no resources
// were available and a series of DPJ based tasks have been built-up in the queue, they would all be executed
// possibly overflowing the system. Therefore, we also include an epsilon for DPJs.
// Note, that if a SBJ is the root for a DPJ, the SBJ can take an arbitrary amount of time,
// the scheduled time t of the child DPJ, will be determined once the parent completes.

trait BaseJob {
  def name: String

  def command: String

  def epsilon: Period = Minutes.minutes(5).toPeriod

  def successCount: Long = 0L

  def errorCount: Long = 0L

  def executor: String = ""

  def executorFlags: String = ""

  def taskInfoData: String = ""

  def retries: Int = 2

  def owner: String = ""

  def ownerName: String = ""

  def description: String = ""

  def lastSuccess: String = ""

  def lastError: String = ""

  def async: Boolean = false

  def cpus: Double = 0

  def disk: Double = 0

  def mem: Double = 0

  def disabled: Boolean = false

  def errorsSinceLastSuccess: Long = 0L

  @Deprecated
  def uris: Seq[String] = List()

  def fetch: Seq[Fetch] = List()

  def highPriority: Boolean = false

  def runAsUser: String = ""

  def container: Container = null

  def environmentVariables: Seq[EnvironmentVariable] = List()

  def shell: Boolean = true

  def arguments: Seq[String] = List()

  def softError: Boolean = false

  def dataProcessingJobType: Boolean = false

  def constraints: Seq[Constraint] = List()
}

@JsonDeserialize(using = classOf[JobDeserializer])
case class ScheduleBasedJob(
                             @JsonProperty schedule: String,
                             @JsonProperty override val name: String,
                             @JsonProperty override val command: String,
                             @JsonProperty override val epsilon: Period = Minutes.minutes(5).toPeriod,
                             @JsonProperty override val successCount: Long = 0L,
                             @JsonProperty override val errorCount: Long = 0L,
                             @JsonProperty override val executor: String = "",
                             @JsonProperty override val executorFlags: String = "",
                             @JsonProperty override val taskInfoData: String = "",
                             @JsonProperty override val retries: Int = 2,
                             @JsonProperty override val owner: String = "",
                             @JsonProperty override val ownerName: String = "",
                             @JsonProperty override val description: String = "",
                             @JsonProperty override val lastSuccess: String = "",
                             @JsonProperty override val lastError: String = "",
                             @JsonProperty override val async: Boolean = false,
                             @JsonProperty override val cpus: Double = 0,
                             @JsonProperty override val disk: Double = 0,
                             @JsonProperty override val mem: Double = 0,
                             @JsonProperty override val disabled: Boolean = false,
                             @JsonProperty override val errorsSinceLastSuccess: Long = 0L,
                             @Deprecated @JsonProperty override val uris: Seq[String] = List(),
                             @JsonProperty override val fetch: Seq[Fetch] = List(),
                             @JsonProperty override val highPriority: Boolean = false,
                             @JsonProperty override val runAsUser: String = "",
                             @JsonProperty override val container: Container = null,
                             @JsonProperty scheduleTimeZone: String = "",
                             @JsonProperty override val environmentVariables: Seq[EnvironmentVariable] = List(),
                             @JsonProperty override val shell: Boolean = true,
                             @JsonProperty override val arguments: Seq[String] = List(),
                             @JsonProperty override val softError: Boolean = false,
                             @JsonProperty override val dataProcessingJobType: Boolean = false,
                             @JsonProperty override val constraints: Seq[Constraint] = List())
  extends BaseJob


@JsonDeserialize(using = classOf[JobDeserializer])
case class DependencyBasedJob(
                               @JsonProperty parents: Set[String],
                               @JsonProperty override val name: String,
                               @JsonProperty override val command: String,
                               @JsonProperty override val epsilon: Period = Minutes.minutes(5).toPeriod,
                               @JsonProperty override val successCount: Long = 0L,
                               @JsonProperty override val errorCount: Long = 0L,
                               @JsonProperty override val executor: String = "",
                               @JsonProperty override val executorFlags: String = "",
                               @JsonProperty override val taskInfoData: String = "",
                               @JsonProperty override val retries: Int = 2,
                               @JsonProperty override val owner: String = "",
                               @JsonProperty override val ownerName: String = "",
                               @JsonProperty override val description: String = "",
                               @JsonProperty override val lastSuccess: String = "",
                               @JsonProperty override val lastError: String = "",
                               @JsonProperty override val async: Boolean = false,
                               @JsonProperty override val cpus: Double = 0,
                               @JsonProperty override val disk: Double = 0,
                               @JsonProperty override val mem: Double = 0,
                               @JsonProperty override val disabled: Boolean = false,
                               @JsonProperty override val errorsSinceLastSuccess: Long = 0L,
                               @Deprecated @JsonProperty override val uris: Seq[String] = List(),
                               @JsonProperty override val fetch: Seq[Fetch] = List(),
                               @JsonProperty override val highPriority: Boolean = false,
                               @JsonProperty override val runAsUser: String = "",
                               @JsonProperty override val container: Container = null,
                               @JsonProperty override val environmentVariables: Seq[EnvironmentVariable] = List(),
                               @JsonProperty override val shell: Boolean = true,
                               @JsonProperty override val arguments: Seq[String] = List(),
                               @JsonProperty override val softError: Boolean = false,
                               @JsonProperty override val dataProcessingJobType: Boolean = false,
                               @JsonProperty override val constraints: Seq[Constraint] = List())
  extends BaseJob
