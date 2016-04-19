# Chronos [![Build Status](https://travis-ci.org/mesos/chronos.svg?branch=master)](https://travis-ci.org/mesos/chronos)
Chronos is a replacement for `cron`. It is a distributed and fault-tolerant scheduler that runs on top of [Apache Mesos][mesos] that can be used for job orchestration.  It supports custom Mesos executors as well
as the default command executor. Thus by default, Chronos executes `sh`
(on most systems bash) scripts.

Chronos can be used to interact with systems such as Hadoop (incl. EMR), even if the Mesos slaves on which execution happens do not have Hadoop installed. Included wrapper scripts allow transfering files and executing them on a remote machine in the background and using asynchronous callbacks to notify Chronos of job completion or failures. Chronos is also natively able to schedule jobs that run inside Docker containers.

Chronos has a number of advantages over regular cron.
It allows you to schedule your jobs using [ISO8601][ISO8601] repeating interval notation, which enables more flexibility in job scheduling. Chronos also supports the definition of jobs triggered by the completion of other jobs. It supports arbitrarily long dependency chains.

*The easiest way to use Chronos is to use [DC/OS](https://dcos.io/get-started/) and install chronos via the universe.*


## Features

* Web UI
* 8601 Repeating Interval Notation
* Handles dependencies
* Job Stats (e.g. 50th, 75th, 95th and 99th percentile timing, failure/success)
* Job History (e.g. job duration, start time, end time, failure/success)
* Fault Tolerance (Hot Master)
* Configurable Retries
* Multiple Workers (i.e. Mesos Slaves)
* Native Docker support

## Documentation and Support

Chronos documentation is available on the [Chronos GitHub pages site](https://mesos.github.io/chronos/).

Documentation for installing and configuring the full Mesosphere stack including Mesos and Chronos is available on the [Mesosphere website](https://docs.mesosphere.com).

For questions and discussions around Chronos, please use the Google Group "chronos-scheduler":
[Chronos Scheduler Group](https://groups.google.com/forum/#!forum/chronos-scheduler).

Also join us on IRC in #chronos on freenode.

If you'd like to take part in design research and test new features in Chronos before they're released, please add your name to Mesosphere's [UX Research](http://uxresearch.mesosphere.com) list.

## Known Issues

The GUI will often drop defined fields (e.g., uri, mem, cpu) when saving a job. We strongly recommend to update jobs *only* through the REST API until issue #426 is fixed.

## Contributing

Instructions on how to contribute to Chronos are available on the [Contributing](http://mesos.github.io/chronos/docs/contributing.html) docs page.

## License

The use and distribution terms for this software are covered by the
Apache 2.0 License (http://www.apache.org/licenses/LICENSE-2.0.html)
which can be found in the file LICENSE at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.

## Contributors

* Florian Leibert ([@flo](http://twitter.com/flo))
* Andy Kramolisch ([@andykram](https://github.com/andykram))
* Harry Shoff ([@hshoff](https://twitter.com/hshoff))
* Elizabeth Lingg

## Reporting Bugs

Please see the [support page](http://mesos.github.io/chronos/support.html) for information on how to report bugs.

[ISO8601]: http://en.wikipedia.org/wiki/ISO_8601 "ISO8601 Standard"
[mesos]: https://mesos.apache.org/ "Apache Mesos"
