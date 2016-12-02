---
title: Fault tolerant job scheduler for Mesos
---

<div class="jumbotron text-center">
  <h1>Chronos</h1>
  <p class="lead">
    A fault tolerant job scheduler for Mesos which handles dependencies and ISO8601 based schedules
  </p>
</div>

## Overview

Chronos is a replacement for `cron`. It is a distributed and fault-tolerant scheduler that runs on top of [Apache Mesos][mesos] that can be used for job orchestration.  It supports custom Mesos executors as well
as the default command executor. Thus by default, Chronos executes `sh`
(on most systems bash) scripts.

Chronos can be used to interact with systems such as Hadoop (incl. EMR), even if the Mesos slaves on which execution happens do not have Hadoop installed. Included wrapper scripts allow transfering files and executing them on a remote machine in the background and using asynchronous callbacks to notify Chronos of job completion or failures. Chronos is also natively able to schedule jobs that run inside Docker containers.

Chronos has a number of advantages over regular cron.
It allows you to schedule your jobs using [ISO8601][ISO8601] repeating interval notation, which enables more flexibility in job scheduling. Chronos also supports the definition of jobs triggered by the completion of other jobs. It supports arbitrarily long dependency chains.

## Sample Architecture

![architecture]({{site.baseurl}}/img/emr_use_case.png "sample architecture")


## Chronos UI

Chronos comes with a UI which can be used to add, delete, list, modify and run jobs. It can also show a graph of job dependencies.
The screenshot should give you a good idea of what Chronos can do.

![Chronos UI screenshot]({{site.baseurl}}/img/chronos_ui-1.png "Chronos UI overview")

![Chronos UI screenshot new job]({{site.baseurl}}/img/chronos_ui-new-job.png "Chronos UI new job")

Additionally, Chronos can show statistics on past job execution. This may include aggregate statistics such as number of
successful and failed executions. Per job execution statistics (i.e. duration and status) are also available, if a
[Cassandra cluster](https://github.com/mesosphere/cassandra-mesos/) is attached to Chronos. Please see the [Configuration
]({{ site.baseurl }}/docs/configuration.html) section
on how to do this.

## Installation

Chronos can be installed on DC/OS using the following command:

    $ dcos package install chronos

Additionally, Mesosphere publishes public Docker images for Chronos. Images are available at <https://hub.docker.com/r/mesosphere/chronos/>. To run Chronos with Docker, you must have 2 ports available: one for the HTTP API, and one for libprocess. You must export these ports as environment variables for Chronos to start. For example:

    $ docker run --net=host -e PORT0=8080 -e PORT1=8081 mesosphere/chronos:v3.0.0 --zk_hosts 192.168.65.90:2181 --master zk://192.168.65.90:2181/mesos


[ISO8601]: http://en.wikipedia.org/wiki/ISO_8601 "ISO8601 Standard"
[mesos]: https://mesos.apache.org/ "Apache Mesos"
