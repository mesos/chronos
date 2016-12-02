---
title: REST API
---

# REST API

You can communicate with Chronos using a RESTful [JSON][] API over HTTP.
Chronos nodes usually listen on port 8080 for API requests.
All examples in this section assume that you've found a running leader at `chronos-node:8080`.

- [Leaders](#leaders)
- [Listing Jobs](#listing-jobs)
- [Searching for a Job](#searching-for-a-job)
- [Deleting a Job](#deleting-a-job)
- [Killing All Tasks for a Job](#killing-all-tasks-for-a-job)
- [Manually Starting a Job](#manually-starting-a-job)
- [Adding a Scheduled Job](#adding-a-scheduled-job)
- [Adding a Dependent Job](#adding-a-dependent-job)
- [Adding a Docker Job](#adding-a-docker-job)
- [Updating Task Progress](#updating-task-progress)
- [Describing the Dependency Graph](#describing-the-dependency-graph)
- [Asynchronous Jobs](#asynchronous-jobs)
- [Obtaining Remote Executables](#obtaining-remote-executables)
- [Job Configuration](#job-configuration)
- [Sample Job](#sample-job)
- [Constraints](#constraints)


## Leaders

When you have multiple Chronos nodes running, only one of them will be elected as the leader.
The leader is the only node that responds to API requests, but if you attempt to talk to a non-leader your request will automatically be redirected to a leader.

To get the current leader you can hit the following endpoint.

* Endpoint: __/leader__
* Method: __GET__
* Example: `curl -L chronos-node:8080/leader`
* Response: A JSON dict containing a single `leader` key.

## Listing Jobs

* Endpoint: __/v1/scheduler/jobs__
* Method: __GET__
* Example: `curl -L -X GET chronos-node:8080/v1/scheduler/jobs`
* Response: JSON data

A job listing returns a JSON list containing all of the jobs.
Each job is a JSON hash.
Interesting fields in the hashes are:

* `successCount`: the number of times the job completed successfully
* `errorCount`: the number of times the job failed to complete
* `lastSuccess`: date of the last successful run of the job
* `lastError`: date of the last failed run of the job
* `executor`: auto-determined by Chronos, but will usually be "" for non-async jobs
* `parents`: for dependent jobs, a list of all other jobs that must run before this job will run

If there is a `parents` field there will be no `schedule` field, and vice-versa.

## Searching for a Job

Get the job definition by searching for the following attributes by using the search endpoint:

* `name`: Name of a job.
* `command`: Command to execute.
* `any`: Search term contained in `name` or `command`.
*
* Endpoint: __/v1/scheduler/jobs/search__
* Method: __GET__
* Example: `curl -L -X GET chronos-node:8080/v1/scheduler/jobs/search?name=request_event_counter_hourly`
* Response: HTTP 204

Search term and the desired job attribute will be converted to lower case. It will then be checked if the job attribute contains the term.

## Deleting a Job

Get a job name from the job listing above.

* Endpoint: __/v1/scheduler/job/\<jobName\>__
* Method: __DELETE__
* Example: `curl -L -X DELETE chronos-node:8080/v1/scheduler/job/request_event_counter_hourly`
* Response: HTTP 204

## Killing All Tasks for a Job

Killing tasks for a job is useful if a job gets stuck. Get a job name from the job listing above.

* Endpoint: __/v1/scheduler/task/kill/\<jobName\>__
* Method: __DELETE__
* Example: `curl -L -X DELETE chronos-node:8080/v1/scheduler/task/kill/request_event_counter_hourly`
* Response: HTTP 204

## Manually Starting a Job

You can manually start a job by issuing an HTTP request.

* Endpoint: __/v1/scheduler/job__
* Method: __PUT__
* Query string parameters: `arguments` - optional string with a list of command line arguments that is appended to job's `command`
  * If job's `shell` is `true`, `arguments` will be ignored.
* Example: `curl -L -X PUT chronos-node:8080/v1/scheduler/job/request_event_counter_hourly`
* Example: `curl -L -X PUT chronos-node:8080/v1/scheduler/job/job_name?arguments=-debug`
* Response: HTTP 204

## Marking a job as successful

You can manually mark a job as successful by issuing an HTTP request. If a job is marked successful, the success count
of the job is incremented, the latest successful run time is updated, and all downstream dependencies are handled as if
the job had completed executing the code in a standard run.
the job normally runs.

* Endpoint: ___/v1/scheduler/job/success/<jobname>
* Method: __PUT__
* Query string parameters: `arguments` - jobname to be marked success
* Example: `curl -L -X PUT chronos-node:8080/v1/scheduler/job/success/request_event_counter_hourly`
* Response: boolean (true or false depending on success of request)

## Adding a Scheduled Job

The heart of job scheduling is a JSON POST request.
The JSON hash you send to Chronos should contain the following fields:

* `name`: The job name. Must contain at least one character and may only contain letters (`[a-zA-Z]`), digits (`[0-9]`), dashes (`-`), underscores (`_`), number signs (`#`), periods (`.`), and whitespace (`[ \t\n\x0B\f\r]`). Must match the following regular expression: `([\w\s\.#_-]+)`
* `command`: The actual command that will be executed by Chronos
* `schedule`: The scheduling for the job, in [ISO 8601][] format. Consists of 3 parts separated by `/`:
  * The number of times to repeat the job: `Rn` to repeat `n` times, or `R` to repeat forever
  * The start time of the job. An empty start time means start immediately. Our format is [ISO 8601][]: `YYYY-MM-DDThh:mm:ss.sTZD` (e.g., `1997-07-16T19:20:30.45+01:00`) where:
      * `YYYY` = four-digit year
      * `MM`   = two-digit month (01 = January, etc.)
      * `DD`   = two-digit day of month (01 through 31)
      * `hh`   = two-digit hour in 24-hour time (00 through 23)
      * `mm`   = two-digit minute (00 through 59)
      * `ss`   = two-digit second (00 through 59)
      * `s`    = one or more digits representing a decimal fraction of a second
      * `TZD`  = time zone designator (`Z` for UTC or `+hh:mm` or `-hh:mm` for UTC offset)
  * The run interval, defined following the ["Duration"](https://en.wikipedia.org/wiki/ISO_8601#Durations) component of the ISO 8601 standard. `P` is required. `T` is for distinguishing M(inute) and M(onth)––it is required when specifying Hour/Minute/Second. For example:
      * `P10M`           = 10 months
      * `PT10M`          = 10 minutes
      * `P1Y12M12D`      = 1 year, 12 months, and 12 days
      * `P12DT12M`       = 12 days and 12 minutes
      * `P1Y2M3DT4H5M6S` = 1 year, 2 months, 3 days, 4 hours, and 5 minutes
* `scheduleTimeZone`: The time zone name to use when scheduling the job. Unlike `schedule`, this is specified in the [tz database](https://en.wikipedia.org/wiki/Tz_database) format, not the [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601#Time_zone_designators) format.
  * This field takes precedence over any time zone specified in `schedule`.
  * All system time zones supported by [`java.util.TimeZone#getAvailableIDs()`](http://docs.oracle.com/javase/7/docs/api/java/util/TimeZone.html#getAvailableIDs()) can be used.
    * See [List of tz database time zones](http://en.wikipedia.org/wiki/List_of_tz_database_time_zones).
  * For example, the effective time zone for the following is `Pacific Standard Time`:
    ```json
    {
      "schedule": "R/2014-10-10T18:32:00Z/PT60M",
      "scheduleTimeZone": "PST"
    }
    ```
* `epsilon`: If Chronos misses the scheduled run time for any reason, it will still run the job if the time is within this interval. Epsilon must be formatted like an [ISO 8601 Duration](https://en.wikipedia.org/wiki/ISO_8601#Durations).
* `owner`: The email address of the person responsible for the job
* `async`: Whether the job runs in the background or not

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

* Endpoint: __/v1/scheduler/iso8601__
* Method: __POST__
* Example:
```bash
curl -L -H 'Content-Type: application/json' -X POST -d '{json hash}' chronos-node:8080/v1/scheduler/iso8601
```
* Response: HTTP 204

## Adding a Dependent Job

A dependent job takes the same JSON format as a scheduled job.
However, instead of the `schedule` field, it accepts a `parents` field.
This should be a JSON list of all jobs which must run at least once before this job will run.

* Endpoint: __/v1/scheduler/dependency__
* Method: __POST__
* Example:
```bash
curl -L -X POST -H 'Content-Type: application/json' -d '{dependent hash}' chronos-node:8080/v1/scheduler/dependency
```
* Response: HTTP 204

Here is a more elaborate example of a dependent job hash:

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

## Adding a Docker Job

A docker job takes the same format as a scheduled job or a dependency job and runs on a Docker container.
To configure it, an additional `container` argument is required, which contains a type (required), an image (required), a network mode (optional), mounted volumes (optional), parameters (optional) and whether Mesos should always pull the latest image before executing or not (optional).

* Endpoint: __/v1/scheduler/iso8601__ or __/v1/scheduler/dependency__
* Method: __POST__
* Example:
```bash
curl -L -H 'Content-Type: application/json' -X POST -d '{json hash}' chronos-node:8080/v1/scheduler/iso8601
```

```json
{
  "schedule": "R/2014-09-25T17:22:00Z/PT2M",
  "name": "dockerjob",
  "container": {
    "type": "DOCKER",
    "image": "libmesos/ubuntu",
    "network": "BRIDGE",
    "volumes": [
      {
        "containerPath": "/var/log/",
        "hostPath": "/logs/",
        "mode": "RW"
      }
    ]
  },
  "cpus": "0.5",
  "mem": "512",
  "fetch": [],
  "command": "while sleep 10; do date =u %T; done"
}
```

Mesos 0.22.0 added support for forcibly pulling the latest version of your
Docker image before launching the task, and this behavior can be enabled in
Chronos by adding the `forcePullImage` boolean to your `container` configuration.

```json
{
  "container": {
    "type": "DOCKER",
    "image": "libmesos/ubuntu",
    "forcePullImage": true
  }
}
```

Chronos will default to not doing a `docker pull` if the image is already found on the executing node. The alternative approach is to use versions/tags for
your images.

There is also support for passing in arbitrary docker config options.

```json
{
  "container": {
    "type": "DOCKER",
    "image": "libmesos/ubuntu",
    "parameters": [
      { "key": "a-docker-option", "value": "xxx" },
      { "key": "b-docker-option", "value": "yyy" }
    ]
}
```

## Updating Task Progress

Task progress can be updated by providing the number of additional elements processed. This will increment the existing count of elements processed.
A job name, task id, and number of additional elements (`numAdditionalElementsProcessed`) is required to update.
This API endpoint requires Cassandra to be present in the cluster.

* Endpoint: __/v1/scheduler/job/\<jobName\>/task/\<taskId\>/progress__
* Method: __POST__
* Example:
```bash
curl -L -H 'Content-Type: application/json' -X POST -d '{json hash}' chronos-node:8080/v1/scheduler/job/NewJob/task/ct%3A1428515194358%3A0%3ANewJob%3A/progress
```

```json
{
  "numAdditionalElementsProcessed": 5
}
```

## Describing the Dependency Graph

Chronos allows describing the dependency graph and has an endpoint to return this graph in the form of a dotfile.

* Endpoint: __/v1/scheduler/graph/dot__
* Method: __GET__
* Example: `curl -L -X GET chronos-node:8080/v1/scheduler/graph/dot`

## Asynchronous Jobs

If your job is long-running, you may want to run it asynchronously.
In this case, you need to do two things:

1. When adding your job, ensure it is set as asynchronous.
2. Your job, when complete, should report its completion status to Chronos.

If you forget to do (2), your job will never run again because Chronos will think that it is still running.
Reporting job completion to Chronos is done via another API call:

* Endpoint: __/v1/scheduler/task/\<task id\>__
* Method: __PUT__
* Example:
```bash
curl -L -X PUT -H "Content-Type: application/json" -d '{"statusCode": 0}' chronos-node:8080/v1/scheduler/task/my_job_run_555_882083xkj302
```

The task id is auto-generated by Chronos. It will be available in your job's environment as `$mesos_task_id`.

_Note_: You will probably need to url-encode the Mesos task id in order to submit it as part of the URL.

## Obtaining Remote Executables

When specifying the `command` field in your job hash, use `url-runner.bash` (make sure it's deployed on all slaves). Alternatively, you can also use a url in the `command` field, if your Mesos was compiled with cURL libraries.

## Job Configuration

| Field                 | Description                                                                                              | Default                        |
| --------------------- |----------------------------------------------------------------------------------------------------------| -------------------------------|
| `name`                | Name of job.                                                                                             | -                              |
| `description`         | Description of job.                                                                                      | -                              |
| `command`             | Command to execute.                                                                                      | -                              |
| `arguments`           | Arguments to pass to the command.  Ignored if `shell` is `true`                                            | -                              |
| `shell`               | If `true`, Mesos will execute `command` by running `/bin/sh -c <command>` and will ignore `arguments`. If `false`, `command` will be treated as the filename of an executable and `arguments` will be the arguments passed.  If this is a Docker job and `shell` is `true`, the entrypoint of the container will be overridden with `/bin/sh -c`                  | `true`                         |
| `executor`            | Mesos executor.  By default Chronos uses the Mesos command executor.                                     | -                              |
| `executorFlags`       | Flags to pass to Mesos executor.                                                                         | -                              |
| `taskInfoData`        | Data to pass to the taskInfo data field.  If set, this overrides the default data set by Chronos.   | -                              |
| `retries`             | Number of retries to attempt if a command returns a non-zero status                                      | `2`                            |
| `owner`               | Email address(es) to send job failure notifications.  Use comma-separated list for multiple addresses.     | -                              |
| `ownerName`           | Name of the individual responsible for the job.                                                          | -                              |
| `successCount`        | Number of successes since the job was last modified.                                                     | -                              |
| `errorCount`          | Number of errors since the job was last modified.                                                        | -                              |
| `lastSuccess`         | Date of last successful attempt.                                                                         | -                              |
| `lastError`           | Date of last failed attempt.                                                                             | -                              |
| `cpus`                | Amount of Mesos CPUs for this job.                                                                       | `0.1` or `--mesos_task_cpu`    |
| `mem`                 | Amount of Mesos Memory (in MB) for this job.                                                               | `128` or `--mesos_task_mem`    |
| `disk`                | Amount of Mesos disk (in MB) for this job.                                                                 | `256` or `--mesos_task_disk`   |
| `disabled`            | If set to `true`, this job will not be run.                                                              | `false`                        |
| `concurrent`          | If set to `true`, this job may execute concurrently (multiple instances).                                                             | `false`                        |
| `uris`                | An array of URIs which Mesos will download when the task is started (deprecated).                         | -                             |
| `fetch`               | An array of fetch configurations, one for each file that Mesos Fetcher will download when the task is started).| -                        |
| `schedule`            | [ISO 8601][] repeating schedule for this job.  If specified, `parents` must not be specified.            | -                              |
| `scheduleTimeZone`    | The time zone for the given schedule, specified in the [tz database](https://en.wikipedia.org/wiki/Tz_database) format. | -                              |
| `parents`             | An array of parent jobs for a dependent job.  If specified, `schedule` must not be specified.            | -                              |
| `runAsUser`           | Mesos will run the job as this user, if specified.                                                       | `--user`                       |
| `container`           | This contains the subfields for the Docker container: `type` (required), `image` (required), `forcePullImage` (optional), `network` (optional), and `volumes` (optional).          | -                              |
| `dataJob`             | Toggles whether the job tracks data (number of elements processed)                                       | `false`                        |
| `environmentVariables`| An array of environment variables passed to the Mesos executor. For Docker containers, these are also passed to Docker using the `-e` flag. | -                              |
| `constraints`         | Control where jobs run. Each constraint is compared against the [attributes of a Mesos slave](http://mesos.apache.org/documentation/attributes-resources/). See [Constraints](#constraints). | -                              |

## Sample Job

```json
{
  "name": "camus_kafka2hdfs",
  "command": "/srv/data-infra/kafka/camus/kafka_hdfs_job.bash",
  "arguments": [
    "-verbose",
    "-debug"
  ],
  "shell": false,
  "epsilon": "PT30M",
  "executor": "",
  "executorFlags": "",
  "retries": 2,
  "owner": "bofh@your-company.com",
  "async": false,
  "successCount": 190,
  "errorCount": 3,
  "lastSuccess": "2014-03-08T16:57:17.507Z",
  "lastError": "2014-03-01T00:10:15.957Z",
  "cpus": 1.0,
  "disk": 10240,
  "mem": 1024,
  "disabled": false,
  "fetch": [
    {
      "uri": "https://url-to-file",
      "cache": false,
      "extract": false,
      "executable": false
    }
  ],
  "schedule": "R/2014-03-08T20:00:00.000Z/PT2H",
  "environmentVariables": [
    {
      "name": "JVMOPTS",
      "value": "-Xmx1000m"
    },
    {
      "name": "JAVA_LIBRARY_PATH",
      "value": "/usr/local/lib"
    }
  ]
}
```

## Constraints

These constraints will work against attributes that are specifically set on the Mesos slaves [as described in the Mesos documentation](http://mesos.apache.org/documentation/latest/configuration).

If a `hostname` attribute is not explicitly specified, one will automatically be created and made available for constraints. It should be noted that calling out specific hostnames is not resilient to slave failure and should be avoided if possible.

### EQUALS constraint

Schedule a job on nodes that share a common attribute.

```json
{
  ...
  "constraints": [["rack", "EQUALS", "rack-1"]],
  ...
}
```

### LIKE constraint

Schedule jobs on nodes which attributes match a regular expression.

```json
{
  ...
  "constraints": [["rack", "LIKE", "rack-[1-3]"]],
  ...
}
```

**Note:** This constraint applies to attributes of type `text` and `scalar` and elements in a `set`, but not `range`.

### UNLIKE constraint

Schedule jobs on nodes which attributes *do not* match a regular expression.

```json
{
  ...
  "constraints": [["rack", "UNLIKE", "rack-[1-3]"]],
  ...
}
```

**Note:** This constraint applies to attributes of type `text` and `scalar` and elements in a `set`, but not `range`.

[json]: http://www.json.org/
[ISO 8601]: https://en.wikipedia.org/wiki/ISO_8601
