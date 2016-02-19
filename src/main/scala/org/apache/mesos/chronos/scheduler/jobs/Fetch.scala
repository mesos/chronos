package org.apache.mesos.chronos.scheduler.jobs

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created by Sylvain Veyrié on 17/02/2016.
  */
case class Fetch(
                  @JsonProperty uri: String,
                  @JsonProperty executable: Boolean = false,
                  @JsonProperty cache: Boolean = false,
                  @JsonProperty extract: Boolean = false)
