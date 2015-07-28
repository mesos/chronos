---
title: REST API
---

# REST API

You can communicate with Chronos using a RESTful [JSON][] API over HTTP.
Chronos nodes usually listen on `port 8080` for API requests.
All examples in this section assume that you've found a running leader at `chronos-node:8080`.

- [Leader](#leaders)
- [Listing Jobs](#listing-jobs)
- [Deleting a Job](#deleting-a-job)
- [Deleting All Tasks for a Job](#deleting-all-tasks-for-a-job)
- [Manually Starting a Job](#manually-starting-a-job)
- [Adding a Scheduled Job](#adding-a-scheduled-job)
- [Adding a Dependent Job](#adding-a-dependent-job)
- [Adding a Docker Job] (#adding-a-docker-job)
- [Updating task progress] (#updating-task-progress)
- [Describing the Dependency Graph](#describing-the-dependency-graph)
- [Asynchronous Jobs](#asynchronous-jobs)
- [Obtaining Remote Executables](#obtaining-remote-executables)
- [Job Configuration](#job-configuration)
- [Sample Job](#sample-job)


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
  * If job's `shell` is true `arguments` will be ignored.
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
  * All system time zones supported by [`java.util.TimeZone#getAvailableIDs()`](http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html#getAvailableIDs()) can be used.
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
To configure it, an additional container argument is required, which contains a type (req), an image (req), a network mode (optional), mounted volumes (optional) and if mesos should always pull latest image before executing (optional).

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
  "network": "BRIDGE",
  "volumes": [{"containerPath": "/var/log/", "hostPath":"/logs/", "mode":"RW"}]
 },
 "cpus": "0.5",
 "mem": "512",
 "uris": [],
 "command": "while sleep 10; do date =u %T; done"
}
```

Mesos 0.22.0 added support to forcably pulling the latest version of your
Docker image before launching the task, and this behavious can be enabled in
Chronos by adding the `forcePullImage` boolean to your container configuration.

```json
{
  "container": {
    "type": "DOCKER",
    "image": "libmesos/ubuntu",
    "forcePullImage": true
  }
}
```

Chronos will default to not doing a `docker pull` if the image is already found
on the executing node. The alternative approach is to use versions/tags for
your images.

###Updating Task Progress

Task progress can be updated by providing the number of additional elements processed. This will increment the existing count of elements processed.
A job name, task id, and number of additional elements (numAdditionalElementsProcessed) is required to update.
This API endpoint requires Cassandra to be present in the cluster.

* Endpoint: __/scheduler/job/<jobName>/task/<taskId>/progress__
* Method: __POST__
* Example:

        curl -L -H 'Content-Type: application/json' -X POST -d '{json hash}' chronos-node:8080/scheduler/job/NewJob/task/ct%3A1428515194358%3A0%3ANewJob%3A/progress

```json
{
    "numAdditionalElementsProcessed": 5
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
| description         | Description of job.                                                                                      | -                              |
| command             | Command to execute.                                                                                      | -                              |
| arguments           | Arguments to pass to the command.  Ignored if `shell` is true                                            | -                              |
| shell               | If true, Mesos will execute `command` by running `/bin/sh -c <command>` and ignore `arguments`. If false, `command` will be treated as the filename of an executable and `arguments` will be the arguments passed.  If this is a Docker job and `shell` is true, the entrypoint of the container will be overridden with `/bin/sh -c`                                                                                      | true                              |
| epsilon             | If, for any reason, a job can't be started at the scheduled time, this is the window in which Chronos will attempt to run the job again | `PT60S` or `--task_epsilon`. |
| executor            | Mesos executor.  By default Chronos uses the Mesos command executor.                                     | -                              |
| executorFlags       | Flags to pass to Mesos executor.                                                                         | -                              |
| retries             | Number of retries to attempt if a command returns a non-zero status                                      | `2`                            |
| owner               | Email addresses to send job failure notifications.  Use comma-separated list for multiple addresses.     | -                              |
| ownerName           | Name of the individual responsible for the job.                                                          | -                              |
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
| scheduleTimeZone    | The time zone for the given schedule.                                    | -                              |
| parents             | An array of parent jobs for a dependent job.  If specified, `schedule` must not be specified.            | -                              |
| runAsUser           | Mesos will run the job as this user, if specified.                                                       | `--user`                       |
| container           | This contains the subfields for the container, type (req), image (req), forcePullImage (optional), network (optional) and volumes (optional).          | -                              |
| dataJob             | Toggles whether the job tracks data (number of elements processed)                                       | `false`                        |
| environmentVariables| An array of environment variables passed to the Mesos executor. For Docker containers, these are also passed to Docker using the -e flag. | -                              |
| constraints         | Control where jobs run. Each constraint is compared against the [attributes of a Mesos slave](http://mesos.apache.org/documentation/attributes-resources/). See [Constraints](#constraints). | - |

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
     {"name": "JVMOPTS", "value": "-Xmx1000m"},
     {"name": "JAVA_LIBRARY_PATH", "value": "/usr/local/lib"}
   ]
}
```

## Constraints

### EQUALS constraint

Schedule a job on nodes that share a common attribute.

```json
...
"constraints": [["rack", "EQUALS", "rack-1"]],
...
```

### LIKE constraint

Schedule jobs on nodes which attributes match a regular expression.

```json
...
"constraints": [["rack", "LIKE", "rack-[1-3]"]],
...
```

**Note:** This constraint applies to attributes of type `text` and `scalar` and elements in a `set`, but not `range`.

[json]: http://www.json.org/
