package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty

case class Fetch(@JsonProperty uri: String,
                 @JsonProperty executable: Boolean = false,
                 @JsonProperty cache: Boolean = false,
                 @JsonProperty extract: Boolean = true)
