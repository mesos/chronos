package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents an environment variable definition for the job
 */
case class EnvironmentVariable(
                                @JsonProperty name: String,
                                @JsonProperty value: String)
