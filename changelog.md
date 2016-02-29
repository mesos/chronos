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
* #637 - Support Mesos fetcher cache
