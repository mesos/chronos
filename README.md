# Chronos

Chronos is AirBnB's replacement for `cron`.
It is a distributed, fault-tolerant system which runs on top of [mesos](http://incubator.apache.org/mesos/).

Chronos has a number of advantages over regular cron.
It allows you to schedule your jobs using [ISO8601][] time specifications, which enables more flexibility in job scheduling.
Chronos also supports the definition of jobs which depend on other jobs to complete successfully first.

**Reference**
> [mesos](http://incubator.apache.org/mesos/) @ [http://incubator.apache.org/mesos/](http://incubator.apache.org/mesos/)
> [ISO8601](http://en.wikipedia.org/wiki/ISO_8601) @ [http://en.wikipedia.org/wiki/ISO_8601](http://en.wikipedia.org/wiki/ISO_8601)

## License

The use and distribution terms for this software are covered by the
Apache 2.0 License (http://www.apache.org/licenses/LICENSE-2.0.html)
which can be found in the file LICENSE at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.

## Core contributors

* Florian Leibert ([@flo](http://twitter.com/flo))
* Andy Kramolisch ([@andykram](https://github.com/andykram))
* Harry Shoff ([@hshoff](https://github.com/hshoff))

## Getting started

There is a file called 'installer.bash' that can be found in the bin directory of the repo. Upon execution, this file will compile and install mesos and Chronos.
After successful installation a local version of Chronos with a built-in ZK server is started.

You will need Maven 3.X, a JDK and build tools to get up and running.

If you get an error while compiling [mesos][], please consult

## The API

You can communicate with Chronos using a RESTful [JSON][] API over HTTP.
Chronos nodes usually listen on `port 4400` for API requests.
All examples in this section assume that you've found a running leader at `chronos-leader.airbnb.com:4400`.

### Leaders

When you have multiple Chronos nodes running, only one of them will be elected as the leader.
The leader is the only node that responds to API requests, but if you attempt to talk to a non-leader your request will automatically be redirected to a leader.

### Listing Jobs

* Endpoint: /scheduler/jobs
* Method: GET
* Example: `curl chronos-leader:4400/scheduler/jobs`
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

* Endpoint: /scheduler/job/jobName
* Method: DELETE
* Example: `curl -X DELETE chronos-leader:4400/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

### Deleting All Jobs

Note: *don't do this*.

* Endpoint: /scheduler/jobs
* Method: DELETE
* Example: `curl -X DELETE chronos-leader:4400/scheduler/jobs`
* Response: HTTP 204

### Manually starting a Job

You can manually start a job by issuing an HTTP request.

* Endpoint: /scheduler/job
* Method: PUT
* Example: `http PUT chronos-leader:4400/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

### Adding a Scheduled Job

The heart of job scheduling is a JSON POST request.
The JSON hash you send to Chronos should contain the following fields:
* Name: the name the job will be known by
* Command: the actual command that will be executed by Cronos
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
  "schedule":"R10/2012-10-01T05:52:00Z/PT2S","name":"SAMPLE_JOB1","epsilon":"PT15M",
    "command":"echo 'FOO' >> /tmp/s99","owner":"bob@airbnb.com","async":false}
```

Once you've generated the hash, send it to Chronos like so:

* Endpoint: /scheduler/iso8601
* Method: POST
* Example:
```bash
     curl -X POST -H 'Content-Type: application/json' -d '{json hash}' chronos-leader:4400/scheduler/iso8601
```
* Response: HTTP 204

### Adding a Dependent Job

A dependent job takes the same JSON format as a scheduled job.
However, instead of the `schedule` field, it will accept a `parents` field.
This should be a JSON list of all jobs which must run at least once before this job will run.

* Endpoint: /scheduler/dependency
* Method: POST
* Example:
```bash
    curl -X POST -H 'Content-Type: application/json' -d '{dependent hash}' chronos-leader:4400/scheduler/iso8601
```

### Asynchronous Jobs

If your job is long-running, you may want to run it asynchronously.
In this case, you need to do two things:

1: When adding your job, add `#async` to the name
2: Add an `executor` field to your job hash and set it to `/srv/mesos/utils/async-executor.arx`
3: Your job when complete should report it's completion status to Chronos.

If you forget to do (2), your job will never run again because Chronos will think that it is still running.
Reporting job completion to Chronos is done via another API call:

* Endpoint: /scheduler/task/*task id*
* Method: PUT
* Example:
```bash
    curl -X PUT -H "Content-Type: application/json" -d '{"statusCode":0}' chronos-leader:4400/scheduler/task/my_job_run_555_882083xkj302
```

The task id is auto-generated by chronos.
It will be available in your job's environment as `$mesos_task_id`.
Note: you may need to url-encode the mesos task id in order to submit it as part of the URL.
For example, when deleting an asynchronous job, replace # by %23.

## Execution Helpers

By default, a chronos job runs on an arbitrary node in the mesos cluster.
Often, you will need to run your job on a specific machine, or the program you want will not be present on the mesos node.
In these cases, we've provided a number of wrapper scripts to help you run your job.

### Obtaining Remote Executables

When specifying the `command` field in your job hash, use `run_url.sh`.

### Running Jobs on an EMR cluster

Use `run_emr.sh` for your `command`.

## Debugging Chronos Jobs

Chronos itself logs to `/var/log/hourly` -- if there's something going wrong with the framework itself look here for information.
Individual jobs log with their task id on the mesos slaves.
Look in the hourly log for your job name and the string "ready for launch", or else "job ct:" and your job name.
The job is done when the line in the log says:
`Task with id 'value: TASK_ID`
The output will say either "**FINISHED**" or "**FAILED**".

To find debug logs on the mesos slave, look in `/tmp/mesos/slaves` on the slave instance. For example:
`/tmp/mesos/slaves/`

In that dir, the current slave run is timestamped so look for the most recent.
Under that is a list of frameworks; you're interested in the chronos framework. For example:
`/tmp/mesos/slaves/STAMP/frameworks/`

Under 'executors' in that dir, you'll find the async executor.
Async jobs (using the async executor) log their output to the stdout/stderr files for that executor. For example, when using the async-executor:
`/tmp/mesos/slaves/STAMP/frameworks/chronos-STAMP/executors/srv/mesos/utils/async-executor.arx/runs/latest`

Regular, non-async jobs will have their own dir under the executors dir; look for their logs there.

## Notes

The cron scheduler allows users to submit cron jobs that run in the cluster. When started, the default behavior of the
scheduler is to register an executor that acts as a wrapper around the program that should be executed.

The default executor is the curl executor which can be found in the ./bin directory.
It's simple yet powerful as it lets the user supply a URL to a script which is downloaded and executed as specified via
the cron syntax.

Refer to CronScheduler.scala for the command line options to use a different executor.

The curl executor is even more powerful if the specified URLs are packaged and self-contained executables.
This can be done for example via arx, which bundles code (https://github.com/solidsnack/arx).
Arx applications in turn contain shell commands and an archive (e.g. a jar file and a startup-script).
It's easy to use and there are no libraries required to unpack and execute the archive.

Signed URLs can be used to publish arx files (e.g. on s3).
To start a new scheduler you have to give the JVM access to the native mesos library.
You can do so by either setting the `java.library.path` to the build mesos library or create an environment variable `MESOS_NATIVE_LIBRARY` and set it to the `mesoslib.dylib` / `mesoslib.so` file
Also, you have to set `MESOS_LAUNCHER_DIR` to the src of the build of mesos such that the shell executor can be found. (e.g. `/Users/florian/airbnb_code/mesos/build/src`)

Also set `MESOS_KILLTREE` to the location of the killtree.sh script - `/Users/florian/airbnb_code/mesos/src/scripts/killtree.sh`

## Appendix

### Finding a Node to Talk To

As we mentioned, Chronos is designed (not required) to run with multiple nodes of which one is elected master.
First, you need to determine the name of the Chronos leader.

#### Using Zoneify

```bash
abbsrv chronos.g
```

Or if you don't have abbsrv:

```bash
dig +short +tcp _*._*.chronos.g.aws.airbnb.com SRV | cut -d' ' -f 4
```

To find the slave without abbsrv:

```bash
dig +short +tcp _*._*.mesos-slave.g.aws.airbnb.com SRV | cut -d' ' -f 4
```

#### Using Zookeeper

Chronos registers itself with [Zookeeper][] at the location `/airbnb/service/chronos`.
Framework masters are in the subpath `candidates`.
Get the value of any member node under `candidates` to get a Chronos framework node.
You an query any of those nodes -- non-leaders will redirect your request to the current leader automatically.

[json]: http://www.json.org/
[Zookeeper]: http://zookeeper.apache.org/
