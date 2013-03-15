# Chronos

Chronos is AirBnB's replacement for `cron`.
It is a distributed, fault-tolerant system which runs on top of [][mesos].

Chronos has a number of advantages over regular cron.
It allows you to schedule your jobs using [][ISO8601] time specifications, which enables more flexibility in job scheduling.
Chronos also supports the definition of jobs which depend on other jobs to complete successfully first.

* <a href="#Quick Start">Quick Start</a>
* <a href="#License">License</a>
* <a href="#Contributors">Contributors</a>
* <a href="#API">API</a>
  - <a href="#Leaders">Leaders</a>
  - <a href="#Listing Jobs">Listing Jobs</a>
  - <a href="#Deleting a Job">Deleting a Job</a>
  - <a href="#Deleting All Jobs">Deleting All Jobs</a>
  - <a href="#Manually Starting a Job">Manually Starting a Job</a>
  - <a href="#Adding a Scheduled Job">Adding a Scheduled Job</a>
  - <a href="#Adding a Dependent Job">Adding a Dependent Job</a>
  - <a href="#Asynchronous Jobs">Asynchronous Jobs</a>
  - <a href="#Obtaining Remote Executables">Obtaining Remote Executables</a>
* <a href="#Debugging Chronos Jobs">Debugging Chronos Jobs</a>
* <a href="#Notes">Notes</a>
* <a href="#Notes">Appendix</a>
  - <a href="#Finding a Node to Talk to">Finding a Node to Talk to</a>
  - <a href="#Zookeeper">Zookeeper</a>

## Quick Start

There is a file called 'installer.bash' that can be found in the bin directory of the repo. It will compile and install mesos and Chronos.
After successful installation a local version of Chronos with a built-in ZK server is started. You will need Maven 3.X, a JDK and build tools to get up and running.
This is how you run this installer:


    $./bin/installer.bash


If you get an error while compiling [][mesos], please consult the FAQ.

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
* Harry Shoff ([@hshoff](https://github.com/hshoff))

## API

You can communicate with Chronos using a RESTful [JSON][] API over HTTP.
Chronos nodes usually listen on `port 4400` for API requests.
All examples in this section assume that you've found a running leader at `chronos-leader.airbnb.com:4400`.

### Leaders

When you have multiple Chronos nodes running, only one of them will be elected as the leader.
The leader is the only node that responds to API requests, but if you attempt to talk to a non-leader your request will automatically be redirected to a leader.

### Listing Jobs

* Endpoint: __/scheduler/jobs__
* Method: __GET__
* Example: `curl curl -L -X GET chronos-node:4400/scheduler/jobs`
* Response: JSON data

A job listing returns a JSON list containing all of the jobs.
Each job is a JSON hash.
Interesting fields in the hashes are:

* `invocationCount`: the number of times the job completed
* `executor`: auto-determined by chronos, but will usually be "" for non-async jobs
* `parents`: for dependent jobs, a list of all other jobs that must run before this job will do so

If there is a `parents` field there will be no `schedule` field and vice-versa.

### Deleting a Job

Get a job name from the job listing above. Then:

* Endpoint: __/scheduler/job/jobName__
* Method: __DELETE__
* Example: `curl -L -X DELETE chronos-node:4400/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

### Deleting All Jobs

Note: *don't do this*.

* Endpoint: __/scheduler/jobs__
* Method: __DELETE__
* Example: `curl -L -X DELETE chronos-node:4400/scheduler/jobs`
* Response: HTTP 204

### Manually Starting a Job

You can manually start a job by issuing an HTTP request.

* Endpoint: __/scheduler/job__
* Method: __PUT__
* Example: `curl -L chronos-node:4400/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

### Adding a Scheduled Job

The heart of job scheduling is a JSON POST request.
The JSON hash you send to Chronos should contain the following fields:
* Name: the name the job will be known by
* Command: the actual command that will be executed by Chronos
* Schedule: The scheduling for the job, in ISO8601 format. Consists of 3 parts separated by '/':
    * Number of times to repeat the job; put just 'R' to repeat forever
    * The start time of the job
    * The run interval
* Epsilon: If Chronos misses the scheduled run time for any reason, it will still run the job if the time is within this interval.
* Owner: the email address of the person responsible for the job
* Async: whether the job runs in the background

Here is an example job hash:
```json
{
  "schedule":"R10/2012-10-01T05:52:00Z/PT2S",
  "name":"SAMPLE_JOB1",
  "epsilon":"PT15M",
  "command":"echo 'FOO' >> /tmp/s99",
  "owner":"bob@airbnb.com","async":false
}
```

Once you've generated the hash, send it to Chronos like so:

* Endpoint: __/scheduler/iso8601__
* Method: POST
* Example:
```bash
     curl -L -H 'Content-Type: application/json' -X POST -H 'Content-Type: application/json' -d '{json hash}' chronos-node:4400/scheduler/iso8601
```
* Response: HTTP 204

### Adding a Dependent Job

A dependent job takes the same JSON format as a scheduled job.
However, instead of the `schedule` field, it will accept a `parents` field.
This should be a JSON list of all jobs which must run at least once before this job will run.

* Endpoint: __/scheduler/dependency__
* Method: __POST__
* Example:
```bash
    curl -L -X POST -H 'Content-Type: application/json' -d '{dependent hash}' chronos-node:4400/scheduler/iso8601
```

### Asynchronous Jobs

If your job is long-running, you may want to run it asynchronously.
In this case, you need to do two things:

1: When adding your job, add `#async` to the name
2: Add an `executor` field to your job hash and set it to `/srv/mesos/utils/async-executor.arx`
3: Your job when complete should report it's completion status to Chronos.

If you forget to do (2), your job will never run again because Chronos will think that it is still running.
Reporting job completion to Chronos is done via another API call:

* Endpoint: __/scheduler/task/*task id*__
* Method: __PUT__
* Example:
```bash
    curl -X PUT -H "Content-Type: application/json" -d '{"statusCode":0}' chronos-leader:4400/scheduler/task/my_job_run_555_882083xkj302
```

The task id is auto-generated by chronos.
It will be available in your job's environment as `$mesos_task_id`.
Note: you may need to url-encode the mesos task id in order to submit it as part of the URL.

### Obtaining Remote Executables

When specifying the `command` field in your job hash, use the `url-runner.bash` (make sure it's deployed on all slaves). Alternatively,
you can also use a url in the command field, if your mesos was compiled with cURL libraries.

## Debugging Chronos Jobs

Chronos itself can be configured just like [dropwizard-logging][logging] via the configuration file. If there's something going wrong with the framework itself look here for information. Individual jobs log with their task id on the mesos slaves.
Look in the standard out log for your job name and the string "ready for launch", or else "job ct:" and your job name.
The job is done when the line in the log says:
`Task with id 'value: TASK_ID`
The output will say either "**FINISHED**" or "**FAILED**".

To find debug logs on the mesos slave, look in `/tmp/mesos/slaves` on the slave instance (unless you've specifically supplied a different log folder for mesos). For example:

`/tmp/mesos/slaves/`

In that dir, the current slave run is timestamped so look for the most recent.
Under that is a list of frameworks; you're interested in the chronos framework. For example:
`/tmp/mesos/slaves/STAMP/frameworks/`

## Notes

The curl executor is even more powerful if the specified URLs are packaged and self-contained executables.
This can be done for example via arx, which bundles code [][arx].
Arx applications in turn contain shell commands and an archive (e.g. a jar file and a startup-script).
It's easy to use and there are no libraries required to unpack and execute the archive.

Signed URLs can be used to publish arx files (e.g. on s3).

To start a new scheduler you have to give the JVM access to the native mesos library.
You can do so by either setting the `java.library.path` to the build mesos library or create an environment variable `MESOS_NATIVE_LIBRARY` and set it to the `mesoslib.dylib` / `mesoslib.so` file

Also, you have to set `MESOS_LAUNCHER_DIR` to the src of the build of mesos such that the shell executor can be found. (e.g. `/Users/florian/airbnb_code/mesos/build/src`)

Also set `MESOS_KILLTREE` to the location of the killtree.sh script - `/Users/florian/airbnb_code/mesos/src/scripts/killtree.sh`

If you're using the installer script this should be setup for you

## Appendix

### Finding a Node to Talk to

As we mentioned, Chronos is designed (not required) to run with multiple nodes of which one is elected master.
If you use the cURL command line tool, you can use the "-L" flag and hit any chronos node and you will get a
307 REDIRECT to the leader.

### Zookeeper

Chronos registers itself with [][Zookeeper] at the location `/airbnb/service/chronos`.
Framework masters are in the subpath `candidates`.
Get the value of any member node under `candidates` to get a Chronos framework node.
You an query any of those nodes -- non-leaders will redirect your request to the current leader automatically.

[arx]: https://github.com/solidsnack/arx
[ISO8601]: http://en.wikipedia.org/wiki/ISO_8601 "ISO8601 Standard"
[json]: http://www.json.org/
[mesos]: http://incubator.apache.org/mesos/ "Apache Mesos"
[logging]: http://dropwizard.codahale.com/manual/core/#logging
[Zookeeper]: http://zookeeper.apache.org/