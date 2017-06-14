## Changes from 2.4.0 to 2.5.0

### Highlights of this Release

#### Support for Mesos 1.3.0
Chronos now uses `libmesos` 1.3.0, which should be compatible with at least Mesos 1.3.x and 1.2.x.

#### Updated many dependencies
The versions of many dependencies were bumped.

#### Added support for periods in job names
The job names can now contain letters (`[a-zA-Z]`), digits (`[0-9]`), dashes (`-`), underscores
(`_`), number signs (`#`), periods (`.`), and whitespace (`[ \t\n\x0B\f\r]`). They must match the
following regular expression: `([\w\s\.#_-]+)`

#### Better support for custom executors
When using a custom executor, Chronos will now set the executor id to the job name and also include
the URIs in the CommandInfo proto.

#### Added support for specifying Docker parameters
It is now possible to specify arbitrary Docker parameters.
Check the [REST API documentation](https://mesos.github.io/chronos/docs/api.html) for more details.

#### Changed the default framework name
The framework name doesn't include the version number anymore.

#### New API endpoints 
The new `/scheduler/leader` endpoint makes it possible to get the current leader.

It is now possible to mark a job as successful via the `/scheduler/job/success` API endpoint.

If a job is marked successful, the success count +of the job is incremented, the latest successful
run time is updated, and all downstream dependencies are handled as if the job had completed
executing the code in a standard run.

#### Improved constraints
Chronos now supports the `UNLIKE` operator and it is possible to constraint jobs by hostname.

#### Updates to the Dockerfile

#### Various documentation improvements

### Fixed Issues / Merged PRs
- #497 - Don't allow proxying requests to self
- #534 - Add framework ID to log
- #535 - Allow periods in job names
- #538 - Add support for Docker parameters
- #539 - Fix Dockerfile so node works with Ubuntu base image
- #540 - Extend documentation of `/scheduler/jobs` by the possibility to get a single job
- #541 - Add info for `lastSuccess` and `lastError` to docs for listing jobs
- #545 - Document the job name requirements
- #550 - Add a warning to the README
- #551 - Fixes #540 - Document Search Endpoint
- #556 - [docs:api] Add clarity around values available for constraints
- #562 - Fix #561, constraints are not activate with argument
- #563 - Make it possible to constraint jobs by hostname
- #566 - Bump specs2
- #572 - Fix a formatting issue with api doc
- #578 - Fix the job listing lastSuccess and error type
- #583 - Add SCoverage for generating test coverage reports
- #586 - Add Go client to docs
- #587 - Cache maven artifacts with Travis
- #589 - Add Chronos Shuttle to tools section
- #595 - Add a `markJobSuccessful` API call
- #602 - Set executor id to job name instead of static value
- #603 - Add uris to the CommandInfo when a custom executor is set
- #606 - Prevent jobs list from collapsing in small screens
- #621 - Log a hint that constraints are not met for a task
- #626 - Allow Chronos jobs to override taskInfo data
- #629 - Add a job's run time and attempt as environment variables exposed to the job
- #635 - Remove schedules from jobs made dependent when previously scheduled
- #637 - Support Mesos fetcher cache (and other options)
- #687 - Drop level of biggest logging offenders
- #689 - Dedup ScheduleStream by jobName
- #711 - Add headers required for CORS
- #717 - Add support for overriding or adding arguments to a job
- #741 - Add an API endpoint for getting the current leader
- #749 - Support for username / password credentials for Cassandra
- #804 - Add missing `MESOS_TASK_ID` environment variable


## Changes from 2.3.4 to 2.4.0

### Overview

#### New `LIKE` constraint
A new constraint called `LIKE` was introduced. It matches the value of an attribute of a slave against a regular expression.

#### Added support for Docker `forcePullImage`
Mesos 0.22.0 added support to forcably pulling the latest version of your Docker image before launching the task, and this behavious can be enabled in Chronos by adding the `forcePullImage` boolean to your container configuration.

#### Added support for notifying job failures through an HTTP callback
It is now possible to set a HTTP callback (using the new `--http_notification_url` flag). Which Chronos will call this endpoint in the case of job failures.

#### Improve the way in which unused offers are declined, in order to avoid starvation in a multi-framework context

We have observed that running a large number of frameworks could lead to starvation, in which some frameworks would not receive any offers from the Mesos master. This release includes changes to mitigate offer starvation.

This is done by making the amount of time for which an offer will be declined configurable. This value defaults to 5 seconds (Mesos default), but should be set to a higher value in a multi-framework environment, in order to reduce starvation.

If there is need for an offer (e.g., when a new job is added), then all already declined offers will be actively revived.

New flags:

* `--decline_offer_duration` allows configuring the duration for which unused offers are declined.
* `--revive_offers_for_new_jobs` if specified, then revive offers will be called when a job is added or changed.
* `--min_revive_offers_interval` if `--revive_offers_for_new_jobs` is specified, do not call reviveOffers more often than this interval.

#### Updates to the Dockerfile
Changed the default exported port to 8080, Chronos' default web port.

It is now possible to override the binary executed by the ENTRYPOINT script, e.g.: `docker run -it chronos /bin/bash` and similar.

#### Various documentation improvements

### Fixed Issues / Merged PRs
* #345 - Add support for new docker flag (`docker_force_pull`)
* #487 - Specifying `--mesos_role` breaks resource allocation
* #465 - Remove persistence task only after Mesos starts running it
* #473 - Failure Callback
* #525 - Fix JVM crashes when the hostname is not resolvable
* #520 - Chronos sometimes registers using an empty frameworkId after a leader fail over
* #509 - Chronos should set a non-default filter when declining offers
