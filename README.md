# Chronos [![Build Status](https://travis-ci.org/airbnb/chronos.svg?branch=master)](https://travis-ci.org/airbnb/chronos)

__New detailed documentation for Mesos available via the [Mesosphere Website](http://mesosphere.io/docs/)__

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

**Chronos comes as part of Elastic Mesos on Google Compute Engine - try it out: [Elastic Mesos](https://google.mesosphere.io)**

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
    - [Deleting All Tasks for a Job](#deleting-all-tasks-for-a-job)
    - [Manually Starting a Job](#manually-starting-a-job)
    - [Adding a Scheduled Job](#adding-a-scheduled-job)
    - [Adding a Dependent Job](#adding-a-dependent-job)
    - [Adding a Docker Job] (#adding-a-docker-job)
    - [Describing the Dependency Graph](#describing-the-dependency-graph)
    - [Asynchronous Jobs](#asynchronous-jobs)
    - [Obtaining Remote Executables](#obtaining-remote-executables)
    - [Job Configuration](#job-configuration)
    - [Sample Job](#sample-job)
* [Job Management](#job-management)
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
Chronos nodes usually listen on `port 8080` for API requests.
All examples in this section assume that you've found a running leader at `chronos-node.airbnb.com:8080`.

### Leaders

When you have multiple Chronos nodes running, only one of them will be elected as the leader.
The leader is the only node that responds to API requests, but if you attempt to talk to a non-leader your request will automatically be redirected to a leader.

### Listing Jobs

* Endpoint: __/scheduler/jobs__
* Method: __GET__
* Example: `curl -L -X GET chronos-node:8080/scheduler/jobs`
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
* Example: `curl -L -X DELETE chronos-node:8080/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

### Deleting All Tasks for a Job

Deleting tasks for a job is useful if a job gets stuck. Get a job name from the job listing above. Then:

* Endpoint: __/scheduler/task/kill/jobName__
* Method: __DELETE__
* Example: `curl -L -X DELETE chronos-node:8080/scheduler/task/kill/request_event_counter_hourly`
* Response: HTTP 204

### Manually Starting a Job

You can manually start a job by issuing an HTTP request.

* Endpoint: __/scheduler/job__
* Method: __PUT__
* Query string parameters: `arguments` - optional string with a list of command line arguments that is appended to job's `command`
* Example: `curl -L -X PUT chronos-node:8080/scheduler/job/request_event_counter_hourly`
* Example: `curl -L -X PUT chronos-node:8080/scheduler/job/job_name?arguments=-debug`
* Response: HTTP 204

### Adding a Scheduled Job

The heart of job scheduling is a JSON POST request.
The JSON hash you send to Chronos should contain the following fields:
* Name: the job name
* Command: the actual command that will be executed by Chronos
* Schedule: The scheduling for the job, in ISO8601 format. Consists of 3 parts separated by '/':
    * Number of times to repeat the job; put just 'R' to repeat forever
    * The start time of the job, an empty start time means start immediately. Our format is ISO8601:
      
        YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00) where:

     	YYYY = four-digit year

        MM   = two-digit month (01=January, etc.)

     	DD   = two-digit day of month (01 through 31)

     	hh   = two digits of hour (00 through 23) (am/pm NOT allowed)

     	mm   = two digits of minute (00 through 59)

     	ss   = two digits of second (00 through 59)

     	s    = one or more digits representing a decimal fraction of a second

     	TZD  = time zone designator (Z or +hh:mm or -hh:mm)

    * The run interval; defined as follows:
    
        P10M=10 months
        
        PT10M=10 minutes
            
        P1Y12M12D=1 years plus 12 months plus 12 days
            
        P12DT12M=12 days plus 12 minutes
        
        P1Y2M3DT4H5M6S = P(eriod) 1Y(ear)2M(onth)3D(ay) T(ime) 4H(our)5M(inute)6S(econd)
        
        P is required. T is for distinguishing M(inute) and M(onth), it is required when Hour/Minute/Second exists.


* ScheduleTimeZone: The time zone name to use when scheduling the job.
  * This field takes precedence over any time zone specified in Schedule.
  * All system time zones supported by [`java.util.Timezone#getAvailableIDs()`](http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html#getAvailableIDs()) can be used.
    * See [List of tz database time zones](http://en.wikipedia.org/wiki/List_of_tz_database_time_zones).
  * For example, the effective time zone for the following is `Pacific Standard Time`
    * ```json
      {
        "schedule": "R/2014-10-10T18:32:00Z/PT60M",
        "scheduleTimeZone": "PST"
      }
      ```
* Epsilon: If Chronos misses the scheduled run time for any reason, it will still run the job if the time is within this interval. Epsilon must be formatted like an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations).
* Owner: the email address of the person responsible for the job
* Async: whether the job runs in the background

A job without *schedule* field will has a "R1//PT24H" schedule by default.

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

        curl -L -H 'Content-Type: application/json' -X POST -d '{json hash}' chronos-node:8080/scheduler/iso8601

* Response: HTTP 204

### Adding a Dependent Job

A dependent job takes the same JSON format as a scheduled job.
However, instead of the `schedule` field, it will accept a `parents` field.
This should be a JSON list of all jobs which must run at least once before this job will run.

* Endpoint: __/scheduler/dependency__
* Method: __POST__
* Example:

        curl -L -X POST -H 'Content-Type: application/json' -d '{dependent hash}' chronos-node:8080/scheduler/dependency

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

###Adding a Docker Job

A docker job takes the same format as a scheduled job or a dependency job and runs on a docker container.
To configure it, an additional container argument is required, which contains a type (req), an image (req), a network mode (optional) and volumes (optional). 

* Endpoint: __/scheduler/iso8601__ or __/scheduler/dependency__
* Method: __POST__
* Example:

        curl -L -H 'Content-Type: application/json' -X POST -d '{json hash}' chronos-node:8080/scheduler/iso8601

```json
{
 "schedule": "R\/2014-09-25T17:22:00Z\/PT2M",
 "name": "dockerjob",
 "container": {
  "type": "DOCKER",
  "image": "libmesos/ubuntu",
  "network": "BRIDGE"
 },
 "cpus": "0.5",
 "mem": "512",
 "uris": [],
 "command": "while sleep 10; do date =u %T; done"
}
```

### Describing the Dependency Graph

Chronos allows to describe the dependency graph and has an endpoint to return this graph in form of a dotfile.

* Endpoint: __/scheduler/graph/dot__
* Method: __GET__
* Example:
```bash
    curl -L -X GET chronos-node:8080/scheduler/graph/dot
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
    curl -L -X PUT -H "Content-Type: application/json" -d '{"statusCode":0}' chronos-node:8080/scheduler/task/my_job_run_555_882083xkj302
```

The task id is auto-generated by Chronos. It will be available in your job's environment as `$mesos_task_id`.

_Note_: You will probably need to url-encode the mesos task id in order to submit it as part of the URL.

### Obtaining Remote Executables

When specifying the `command` field in your job hash, use the `url-runner.bash` (make sure it's deployed on all slaves). Alternatively,
you can also use a url in the command field, if your mesos was compiled with cURL libraries.

### Job Configuration

| Field               | Description                                                                                              | Default                        |
| ------------------- |----------------------------------------------------------------------------------------------------------| -------------------------------|
| name                | Name of job.                                                                                             | -                              |
| command             | Command to execute.                                                                                      | -                              |
| arguments           | Arguments to pass to the command.  Ignored if `shell` is true                                            | -                              |
| shell               | If true, Mesos will execute `command` by running `/bin/sh -c <command>` and ignore `arguments`. If false, `command` will be treated as the filename of an executable and `arguments` will be the arguments passed.  If this is a Docker job and `shell` is true, the entrypoint of the container will be overridden with `/bin/sh -c`                                                                                      | true                              |
| epsilon             | If, for any reason, a job can't be started at the scheduled time, this is the window in which Chronos will attempt to run the job again | `PT60S` or `--task_epsilon`. |
| executor            | Mesos executor.  By default Chronos uses the Mesos command executor.                                     | -                              |
| executorFlags       | Flags to pass to Mesos executor.                                                                         | -                              |
| retries             | Number of retries to attempt if a command returns a non-zero status                                      | `2`                            |
| owner               | Email addresses to send job failure notifications.  Use comma-separated list for multiple addresses.     | -                              |
| async               | Execute using Async executor.                                                                            | `false`                        |
| successCount        | Number of successes since the job was last modified.                                                     | -                              |
| errorCount          | Number of errors since the job was last modified.                                                        | -                              |
| lastSuccess         | Date of last successful attempt.                                                                         | -                              |
| lastError           | Date of last failed attempt.                                                                             | -                              |
| cpus                | Amount of Mesos CPUs for this job.                                                                       | `0.1` or `--mesos_task_cpu`    |
| mem                 | Amount of Mesos Memory in MB for this job.                                                               | `128` or `--mesos_task_mem`    |
| disk                | Amount of Mesos disk in MB for this job.                                                                 | `256` or `--mesos_task_disk`   |
| disabled            | If set to true, this job will not be run.                                                                | `false`                        |
| uris                | An array of URIs which Mesos will download when the task is started.                                     | -                              |
| schedule            | ISO8601 repeating schedule for this job.  If specified, `parents` must not be specified.                 | -                              |
| scheduleTimeZone    | The time zone for the given schedule.									 							     | -                              |
| parents             | An array of parent jobs for a dependent job.  If specified, `schedule` must not be specified.            | -                              |
| runAsUser           | Mesos will run the job as this user, if specified.                                                       | `--user`                       |
| container           | This contains the subfields for the container, type (req), image (req), network (optional) and volumes (optional).          | -                              |
| environmentVariables| An array of environment variables passed to the Mesos executor. For Docker containers, these are also passed to Docker using the -e flag. | -                              |

### Sample Job

```json
{
   "name":"camus_kafka2hdfs",
   "command":"/srv/data-infra/kafka/camus/kafka_hdfs_job.bash",
   "arguments": [
   	  "-verbose",
   	  "-debug"
   ],
   "shell":"false",
   "epsilon":"PT30M",
   "executor":"",
   "executorFlags":"",
   "retries":2,
   "owner":"bofh@your-company.com",
   "async":false,
   "successCount":190,
   "errorCount":3,
   "lastSuccess":"2014-03-08T16:57:17.507Z",
   "lastError":"2014-03-01T00:10:15.957Z",
   "cpus":1.0,
   "disk":10240,
   "mem":1024,
   "disabled":false,
   "uris":[
   ],
   "schedule":"R/2014-03-08T20:00:00.000Z/PT2H",
   "environmentVariables": [
     {"name": "FOO", "value": "BAR"}
   ]
}
```

## Job Management

For larger installations, the web UI may be insufficient for managing jobs.  At
Airbnb, there are well over 700 production Chronos jobs.  Rather than using the
web UI for making edits, we created a script called `chronos-sync.rb` which can
be used to synchronize configuration from disk to Chronos.  For example, you
may have a Git repository that contains all of the Chronos job configurations,
and then you could run an hourly Chronos job that checks out the repository and
runs `chronos-sync.rb`.

You can initialize the configuration data by running:
```
$ bin/chronos-sync.rb -u http://chronos/ -p /path/to/jobs/config -c
```

After that, you can run the normal sync like this:

```
$ bin/chronos-sync.rb -u http://chronos/ -p /path/to/jobs/config
```

You can also forcefully update the configuration in Chronos from disk by
passing the `-f` or `--force` parameter.  In the example above,
`/path/to/jobs/config` is the path where you would like the configuration data
to live.

Note: `chronos-sync.rb` does not delete jobs.  If you want to remove a job, you must manually remove it using the API or web UI.

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

If the bug is in building Mesos from scratch, please [submit those bugs directly to mesos](https://issues.apache.org/jira/browse/MESOS).

If the bug occurs while running Chronos, please include the following
information:

* The command used to launch Chronos, for example:

        java -cp target/chronos.jar com.airbnb.scheduler.Main <args>

* The version of Mesos you are running.

* The output of

        java -version

## Appendix

### Finding a Node to Talk to

As we mentioned, Chronos is designed (not required) to run with multiple nodes of which one is elected master.
If you use the cURL command line tool, you can use the `-L` flag and hit any Chronos node and you will get a
307 REDIRECT to the leader.

### Zookeeper

Chronos registers itself with [Zookeeper][Zookeeper] at the location `/chronos/state`. This value can be changed via the configuration file.


[arx]: https://github.com/solidsnack/arx
[ISO8601]: http://en.wikipedia.org/wiki/ISO_8601 "ISO8601 Standard"
[json]: http://www.json.org/
[mesos]: https://mesos.apache.org/ "Apache Mesos"
[logging]: http://dropwizard.io/manual/core.html#logging
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
	java -cp target/chronos*.jar com.airbnb.scheduler.Main --master zk://localhost:2181/mesos --zk_hosts localhost:2181
