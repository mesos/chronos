# Configuring Chronos

* [Overview](#overview)
* [Chronos Specific Options](#chronos-specific-options)
* [Other Options](#other-options)
* [Example Configurations](#example-configurations)

## Overview

Configuring Chronos is done by [supplying a YAML
file](/airbnb/chronos#running-chronos). You can override configuration
parameters at the command line by specifying them as system properties,
prefixed by `dw.`.

An example of overriding the `hostname` parameter from the command line
can be seen in [bin/run](/airbnb/chronos/blob/master/bin/run#L21).

## Chronos Specific Options

The following is a list of configuration parameters for Chronos, what
they do and when you should specify them.

* [assets](#assets)
* [defaultJobOwner](#defaultjobowner)
* [failoverTimeoutSeconds](#failovertimeoutseconds)
* [failureRetryDelay](#failureretrydelay)
* [gangliaHostPort](#gangliahostport)
* [gangliaReportIntervalSeconds](#gangliareportintervalseconds)
* [gangliaGroupPrefix](#gangliagroupprefix)
* [hostname](#hostname)
* [mailFrom](#mailfrom)
* [mailPassword](#mailpassword)
* [mailServer](#mailserver)
* [mailUser](#mailuser)
* [master](#master)
* [mesosTaskCpu](#mesostaskcpu)
* [mesosTaskMem](#mesostaskmem)
* [mesosTaskDisk](#mesostaskdisk)
* [mesosRole](#mesosrole)
* [mesosTaskCheckpoint](#mesoscheckpoint)
* [scheduleHorizonSeconds](#schedulehorizonseconds)
* [user](#user)
* [zookeeperCandidateZnode](#zookeepercandidateznode)
* [zookeeperServers](#zookeeperservers)
* [zookeeperStateZnode](#zookeeperstateznode)
* [zookeeperTimeoutMs](#zookeepertimeoutms)

### assets

*Default* `None`

A mapping of assets to override, specified as:

    assets:
      overrides:
        /PATH_TO_OVERRIDE: LOCATION_TO_REPLACE_WITH

An example can be seen in [config/local_cluster_asset_dev.yml](/airbnb/chronos/blob/57c91cd07c975571c2758b0d1293a03fac46347c/config/local_cluster_asset_dev.yml#L8)

**Note**: This should only be used if you're developing assets locally
and want to see your updates reflected immediately.

**Note**: Overridden paths are relative to `$CLASSPATH`

### defaultJobOwner

*Default*: `"flo@airbnb.com"`

Default recipient of all mail notifications.

**Note**: You probably want to change this, as it's doubtful that @florianleibert
wants his inbox spammed.

### failoverTimeoutSeconds

*Default*: `1200`

Failover timeout for the Chronos framework.

### failureRetryDelay

*Default*: `60000`

When a task fails, Chronos will wait *up to* this number of milliseconds
to retry.

**Note**: This parameter should be specified in milliseconds.

### gangliaHostPort

*Default*: `None`
*Example*: `ganglia.example.com:8649`

If configured, will report metrics to Ganglia at the configured
interval.

### gangliaReportIntervalSeconds

*Default*: `60`

Metric reporting interval in seconds.

### gangliaGroupPrefix

*Default*: `''`

Group prefix to use for all reported metrics.

### hostname

*Default*: `"localhost"`

The hostname registered in zookeeper for the given Chronos node. This is necessary
for redirects to work.

**Note**: It's most likely that you want to override this at the command
line in order to properly set each Chronos box's hostname. See
[Overview](#overview) for an example.

### mailFrom

*Default*: `None`

The email address to use for the `From` field.

### mailPassword

*Default*: `None`

The password for [mailUser](#mailUser)

### mailServer

*Default*: `None`

The mail server to use to send notification emails.

### mailUser

*Default*: `None`

The user to send mail as.

### mailSslOn

*Default*: false

Whether or not to enable SSL to send notification emails.

### master

*Default*: `local`

Zookeeper configuration used by mesos, of the form discussed
in [src/java/src/org/apache/mesos/MesosSchedulerDriver.java](https://github.com/apache/mesos/blob/7cef9760892da9c7c062db19fbe1ffac833d77fe/src/java/src/org/apache/mesos/MesosSchedulerDriver.java#L58) and reproduced below:

> The master should be one of:
>
> `host:port`
>
> `zk://host1:port1,host2:port2,.../path`
>
> `zk://username:password@host1:port1,host2:port2,.../path`
>
> `file:///path/to/file` (where `file` contains one of the above)

**Note**: This should be the same configuration you used to boot your
mesos slaves and masters.

### mesosTaskCpu

Number of CPUs per Mesos task

*Default*: 1.0

### mesosTaskMem

Amount of memory, in MiB, per Mesos task

*Default*: 1024

### mesosTaskDisk

Amount of disk space, in MiB, required per Mesos task

*Default*: 1024

### mesosRole

The Mesos role to use for this framework.

*Default*: "*"

### mesosCheckpoint

Enable checkpointing for this framework on Mesos

*Default*: false

### scheduleHorizonSeconds

*Default*: `10`

Horizon (duration) within which jobs should be scheduled in advance.

### user

*Default*: `"root"`

The user to run tasks as on mesos slaves.

### zookeeperCandidateZnode

*Default*: `"/airbnb/service/chronos/candidate"`

The root at which all Chronos nodes will register in order to form a
group.

**Note**: All Chronos machines should have the same
`zookeeperCandidateZnode` so that they can join the same group.

### zookeeperServers

*Default*: `"localhost:2181"`

Reference to the zookeepers used:

* Chronos leader election
* Backing the job and task persistence store

### zookeeperStateZnode

*Default*: `"/airbnb/service/chronos/state"`

The root znode in which Chronos persists its state.

**Note**: All Chronos machines should have the same
`zookeeperStateZnode` so that they can share state.

### zookeeperTimeoutMs

*Default*: `5000`

Timeout for the `ZookeeperState` abstraction.

## Other Options

In addition to the [Chronos Specific
Options](#chronos-specific-options), Chronos also supports all options
provided by Dropwizard.

Please see [Dropwizard's Configuration Defaults](http://dropwizard.codahale.com/manual/core/#configuration-defaults) for more on that.

## Example Configurations

We have included sample configuration files for local Chronos development as well as running Chronos in production.

[`local_cluster_scheduler.yml`](#local_cluster_scheduleryml)
[`local_cluster_asset_dev.yml`](#local_cluster_asset_devyml)
[`local_scheduler.yml`](#local_scheduleryml)
[`local_scheduler_nozk.yml`](#local_scheduler_nozkyml)
[`sample_scheduler.yml`](#sample_scheduleryml)

## `local_cluster_scheduler.yml`
This configuration file assumes you have a mesos slave and master running locally. 

## `local_cluster_asset_dev.yml`
This configuration file is the same as the above ([`local_cluster_scheduler.yml`](#local_cluster_scheduleryml)), but also specifies asset overrides. Asset overrides should only be in your configuration file if you are modifying assets locally. If asset overrides are present in your production config, you will be unable to use the UI, as unpackaged assets are not included in the jar.

## `local_scheduler_nozk.yml`
Very basic configuration file, sufficient for testing Chronos locally. **Never** run such a configuration in production.

## `sample_scheduler.yml`
This configuration file specifies all relevant options in order to get Chronos running in production. Use this as a basis for your Chronos production configuration.
