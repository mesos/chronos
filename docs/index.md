---
title: Fault tolerant job scheduler for Mesos
---

<div class="jumbotron text-center">
  <h1>Chronos</h1>
  <p class="lead">
    A fault tolerant job scheduler for Mesos which handles dependencies and ISO8601 based schedules
  </p>
  <p>
    <a href="https://github.com/mesos/chronos/archive/2.3.4.tar.gz"
        class="btn btn-lg btn-primary">
      Download Chronos v2.3.4
    </a>
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

You can either download an archive of the latest Chronos release using the Download button above or follow the instructions on the [getting started page]({{site.baseurl}}/docs/) to install packages for popular Linux distributions. 


[ISO8601]: http://en.wikipedia.org/wiki/ISO_8601 "ISO8601 Standard"
[mesos]: https://mesos.apache.org/ "Apache Mesos"
