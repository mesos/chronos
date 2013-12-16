# Chronos

Chronos is Airbnb's replacement for `cron`.
It is a distributed and fault-tolerant scheduler that runs on top of [Apache Mesos][mesos].
You can use it to orchestrate jobs. It supports custom Mesos executors as well
as the default command executor. Thus by default, Chronos executes `sh`
(on most systems bash) scripts. Chronos can be used to interact with systems
such as Hadoop (incl. EMR), even if the Mesos slaves on which execution happens
do not have Hadoop installed. Included wrapper scripts allow transfering files
and executing them on a remote machine in the background and using asynchronous
callbacks to notify Chronos of job completion or failures.

Chronos has a number of advantages over regular cron.
It allows you to schedule your jobs using [ISO8601][ISO8601] repeating interval notation, which enables more flexibility in job scheduling.
Chronos also supports the definition of jobs triggered by the completion of other jobs. It supports arbitrarily long dependency chains.

**Try out the interactive and personalized [tutorial for Chronos](http://mesosphere.io/learn/run-chronos-on-mesos/).**

For questions and discussions around Chronos, please use the Google Group "chronos-scheduler":
[Chronos Scheduler Group](https://groups.google.com/forum/#!forum/chronos-scheduler).
Also join us on IRC in #mesos on freenode.


* [Features](#features)
* [Running Chronos](#running-chronos)
* [Configuring Chronos](#configuring-chronos)
* [License](#license)
* [Contributors](#contributors)
* [Video Introduction](#video-introduction)
* [Chronos UI](#chronos-ui)
* [API](#api)
    - [Leader](#leaders)
    - [Listing Jobs](#listing-jobs)
    - [Deleting a Job](#deleting-a-job)
    - [Deleting All Jobs](#deleting-all-jobs)
    - [Deleting All Tasks for a Job](#deleting-all-tasks-for-a-job)
    - [Manually Starting a Job](#manually-starting-a-job)
    - [Adding a Scheduled Job](#adding-a-scheduled-job)
    - [Adding a Dependent Job](#adding-a-dependent-job)
    - [Describing the Dependency Graph](#describing-the-dependency-graph)
    - [Asynchronous Jobs](#asynchronous-jobs)
    - [Obtaining Remote Executables](#obtaining-remote-executables)
* [Debugging Chronos Jobs](#debugging-chronos-jobs)
* [Notes](#notes)
    - [Environment Variables Mesos Looks For](#environment-variables-mesos-looks-for)
* [Reporting Bugs](#reporting-bugs)
* [Appendix](#appendix)
    - [Finding a Node to Talk To](#finding-a-node-to-talk-to)
    - [Zookeeper](#zookeeper)
    - [Install Chronos on Amazon Linux](#install-chronos-on-amazon-linux)



If you get an error while compiling Mesos, please consult the [FAQ](docs/FAQ.md).

## Features

* Web UI
* 8601 Repeating Interval Notation
* Handles dependencies
* Job Stats (e.g. 50th, 75th, 95th and 99th percentile timing, failure/success)
* Fault Tolerance (Hot Master)
* Configurable Retries
* Multiple Workers (i.e. Mesos Slaves)

## Running Chronos

We've included some [example run scripts](#example-run-scripts), but the
basic syntax for launching chronos is:

    java -cp chronos.jar --master zk://127.0.0.1:2181/mesos --zk_hosts 127.0.0.1:2181

Please note that you need to have both Mesos and Zookeeper running for this to work!

For more information on configuration options, please see [configuring
Chronos](#configuring-chronos).

### Example Run Scripts

* Example [runit](http://smarden.org/runit/) run script

    [bin/run](bin/run)

* Example local run script

    [bin/start-chronos.bash](bin/start-chronos.bash)

## Configuring Chronos

For information on configuring chronos, please see [docs/CONFIG.md](docs/CONFIG.md).

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

## Video Introduction

* Replacing Cron & Building Scalable Data Pipelines: [YouTube](http://www.youtube.com/watch?v=FLqURrtS8IA)

## Sample Architecture

![architecture](https://raw.github.com/airbnb/chronos/master/docs/emr_use_case.png "sample architecture")

## Chronos UI

Chronos comes with a UI which can be used to add, delete, list, modify and run jobs. It can also show a graph of job dependencies.
The screenshot should give you a good idea of what Chronos can do.

![Chronos UI screenshot](https://raw.github.com/airbnb/chronos/master/docs/chronos_ui-1.png "Chronos UI overview")

![Chronos UI screenshot new job](https://raw.github.com/airbnb/chronos/master/docs/chronos_ui-new-job.png "Chronos UI new job")

## API

You can communicate with Chronos using a RESTful [JSON][] API over HTTP.
Chronos nodes usually listen on `port 4400` for API requests.
All examples in this section assume that you've found a running leader at `chronos-node.airbnb.com:4400`.

### Leaders

When you have multiple Chronos nodes running, only one of them will be elected as the leader.
The leader is the only node that responds to API requests, but if you attempt to talk to a non-leader your request will automatically be redirected to a leader.

### Listing Jobs

* Endpoint: __/scheduler/jobs__
* Method: __GET__
* Example: `curl -L -X GET chronos-node:4400/scheduler/jobs`
* Response: JSON data

A job listing returns a JSON list containing all of the jobs.
Each job is a JSON hash.
Interesting fields in the hashes are:

* `invocationCount`: the number of times the job completed
* `executor`: auto-determined by Chronos, but will usually be "" for non-async jobs
* `parents`: for dependent jobs, a list of all other jobs that must run before this job will do so

If there is a `parents` field there will be no `schedule` field and vice-versa.

### Deleting a Job

Get a job name from the job listing above. Then:

* Endpoint: __/scheduler/job/jobName__
* Method: __DELETE__
* Example: `curl -L -X DELETE chronos-node:4400/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

### Deleting All Tasks for a Job

Deleting tasks for a job is useful if a job gets stuck. Get a job name from the job listing above. Then:

* Endpoint: __/scheduler/task/kill/jobName__
* Method: __DELETE__
* Example: `curl -L -X DELETE chronos-node:4400/scheduler/task/kill/request_event_counter_hourly`
* Response: HTTP 204

### Manually Starting a Job

You can manually start a job by issuing an HTTP request.

* Endpoint: __/scheduler/job__
* Method: __PUT__
* Example: `curl -L -X PUT chronos-node:4400/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

### Adding a Scheduled Job

The heart of job scheduling is a JSON POST request.
The JSON hash you send to Chronos should contain the following fields:
* Name: the job name
* Command: the actual command that will be executed by Chronos
* Schedule: The scheduling for the job, in ISO8601 format. Consists of 3 parts separated by '/':
    * Number of times to repeat the job; put just 'R' to repeat forever
    * The start time of the job
    * The run interval
* Epsilon: If Chronos misses the scheduled run time for any reason, it will still run the job if the time is within this interval. Epsilon must be formatted like an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations).
* Owner: the email address of the person responsible for the job
* Async: whether the job runs in the background

Here is an example job hash:
```json
{
  "schedule": "R10/2012-10-01T05:52:00Z/PT2S",
  "name": "SAMPLE_JOB1",
  "epsilon": "PT15M",
  "command": "echo 'FOO' >> /tmp/JOB1_OUT",
  "owner": "bob@airbnb.com",
  "async": false
}
```

Once you've generated the hash, send it to Chronos like so:

* Endpoint: __/scheduler/iso8601__
* Method: __POST__
* Example:

        curl -L -H 'Content-Type: application/json' -X POST -d '{json hash}' chronos-node:4400/scheduler/iso8601

* Response: HTTP 204

### Adding a Dependent Job

A dependent job takes the same JSON format as a scheduled job.
However, instead of the `schedule` field, it will accept a `parents` field.
This should be a JSON list of all jobs which must run at least once before this job will run.

* Endpoint: __/scheduler/dependency__
* Method: __POST__
* Example:

        curl -L -X POST -H 'Content-Type: application/json' -d '{dependent hash}' chronos-node:4400/scheduler/iso8601

Here is a more elaborate example for a dependency job hash:

```json
{
    "async": true,
    "command": "bash -x /srv/data-infra/jobs/hive_query.bash run_hive hostings-earnings-summary",
    "epsilon": "PT30M",
    "errorCount": 0,
    "lastError": "",
    "lastSuccess": "2013-03-15T13:02:14.243Z",
    "name": "hostings_earnings_summary",
    "owner": "bob@airbnb.com",
    "parents": [
        "db_export-airbed_hostings",
        "db_export-airbed_reservation2s"
    ],
    "retries": 2,
    "successCount": 100
}
```

### Describing the Dependency Graph

Chronos allows to describe the dependency graph and has an endpoint to return this graph in form of a dotfile.

* Endpoint: __/scheduler/graph/dot__
* Method: __GET__
* Example:
```bash
    curl -L -X GET chronos-node:4400/scheduler/graph/dot
```

### Asynchronous Jobs

If your job is long-running, you may want to run it asynchronously.
In this case, you need to do two things:

1. When adding your job, ensure it is set as asynchronous.
2. Your, job, when complete, should reports its completion status to Chronos.

If you forget to do (2), your job will never run again because Chronos will think that it is still running.
Reporting job completion to Chronos is done via another API call:

* Endpoint: __/scheduler/task/*task id*__
* Method: __PUT__
* Example:
```bash
    curl -L -X PUT -H "Content-Type: application/json" -d '{"statusCode":0}' chronos-node:4400/scheduler/task/my_job_run_555_882083xkj302
```

The task id is auto-generated by Chronos. It will be available in your job's environment as `$mesos_task_id`.

_Note_: You will probably need to url-encode the mesos task id in order to submit it as part of the URL.

### Obtaining Remote Executables

When specifying the `command` field in your job hash, use the `url-runner.bash` (make sure it's deployed on all slaves). Alternatively,
you can also use a url in the command field, if your mesos was compiled with cURL libraries.

## Debugging Chronos Jobs

Chronos itself can be configured just like [dropwizard-logging][logging] via the configuration file. If there's something going wrong with the framework itself look here for information. Individual jobs log with their task id on the mesos slaves.
Look in the standard out log for your job name and the string "ready for launch", or else "job ct:" and your job name.
The job is done when the line in the log says:

    Task with id 'value: TASK_ID **FINISHED**

To find debug logs on the mesos slave, look in `/tmp/mesos/slaves` on the slave instance (unless you've specifically supplied a different log folder for mesos). For example:

    /tmp/mesos/slaves/

In that dir, the current slave run is timestamped so look for the most recent.
Under that is a list of frameworks; you're interested in the Chronos framework.
For example:

    /tmp/mesos/slaves/STAMP/frameworks/

## Notes

The curl executor is even more powerful if the specified URLs are packaged and self-contained executables.
This can be done for example via [arx][arx], which bundles code into an executable archive.
[Arx][arx] applications in turn contain shell commands and an archive (e.g. a jar file and a startup-script).
It's easy to use and there are no libraries required to unpack and execute the archive.

Signed URLs can be used to publish arx files (e.g. on s3).

To start a new scheduler you have to give the JVM access to the native mesos library.
You can do so by either setting the `java.library.path` to the build mesos library or create an environment variable `MESOS_NATIVE_LIBRARY` and set it to the `mesoslib.dylib` / `mesoslib.so` file

### Environment Variables Mesos Looks For

* `MESOS_NATIVE_LIBRARY`: Absolute path to the native mesos library. This is usually `/usr/local/lib/libmesos.so` on Linux and `/usr/local/lib/libmesos.dylib` on OSX.
* `MESOS_LAUNCHER_DIR`: Absolute path to the src subdirectory of your mesos build, such that the shell executor can be found (e.g. If mesos was built in `/Users/florian/airbnb_code/mesos/build` then the value for this variable would be `/Users/florian/airbnb_code/mesos/build/src`).
* `MESOS_KILLTREE`: Absolute path to the location of the `killtree.sh` script. (e.g. `/Users/florian/airbnb_code/mesos/src/scripts/killtree.sh`)

If you're using the installer script this should be setup for you.

## Reporting Bugs

To make all of our lives easier we ask that all bug reports
include at least the following information:

The output of:

    mvn -X clean package

and

    java -version

If the error is in running tests, then please include the output of
running all the tests.

    # Mac/FreeBSD
    tail +1 target/surefire-reports/*.txt
    # GNU Coreutils
    tail -n +1 target/surefire-reports/*.txt

If the error is in the installer, please include all
the output from running it with debug enabled:

    bash -x bin/installer.bash

If the bug is in building mesos from scratch, please [submit those bugs directly to mesos](https://issues.apache.org/jira/browse/MESOS).

If the bug occurs while running Chronos, please include the following
information:

* The command used to launch Chronos, for example:

        java -cp target/chronos.jar com.airbnb.scheduler.Main server config/local_scheduler_nozk.yml

* The YAML file used to configure Chronos.

* The version of Mesos you are running.

* The output of

        java -version

## Appendix

### Finding a Node to Talk to

As we mentioned, Chronos is designed (not required) to run with multiple nodes of which one is elected master.
If you use the cURL command line tool, you can use the `-L` flag and hit any Chronos node and you will get a
307 REDIRECT to the leader.

### Zookeeper

Chronos registers itself with [Zookeeper][Zookeeper] at the location `/airbnb/service/chronos`. This value can be changed via the configuration file.


[arx]: https://github.com/solidsnack/arx
[ISO8601]: http://en.wikipedia.org/wiki/ISO_8601 "ISO8601 Standard"
[json]: http://www.json.org/
[mesos]: https://mesos.apache.org/ "Apache Mesos"
[logging]: http://dropwizard.codahale.com/manual/core/#logging
[Zookeeper]: https://zookeeper.apache.org/


### Install Chronos on Amazon Linux

Follow these steps to install Chronos on Amazon Linux:

##### Install Dependencies

###### Debian Linux:
    sudo apt-get install autoconf make gcc cpp patch python-dev git libtool default-jdk default-jdk-builddep default-jre gzip libghc-zlib-dev libcurl4-openssl-dev

###### Fedora Linux:
    sudo yum install autoconf make gcc gcc-c++ patch python-devel git libtool java-1.7.0-openjdk-devel zlib-devel libcurl-devel openssl-devel cyrus-sasl-devel

Make sure you're using Java 7: `sudo alternatives --config java`

##### Build and Install Mesos

	git clone https://github.com/apache/mesos.git
	cd mesos/
	git checkout 
	export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.45.x86_64/
	./bootstrap
	./configure --with-webui --with-included-zookeeper --disable-perftools --enable-frame-pointers
	make
	sudo make install

##### Build Chronos

Install [Node](http://nodejs.org/) first. On OSX, try `brew install node`.

Start up Zookeeper, Mesos master, and Mesos slave(s).  Then try
	
	export MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so
	git clone https://github.com/airbnb/chronos.git
	cd chronos
	mvn package
	java -cp target/chronos*.jar com.airbnb.scheduler.Main --master zk://localhost:5050/mesos --zk_hosts localhost:2181
