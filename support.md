---
layout: narrow
tab: support
title: Getting Support for Chronos
---

# Getting Support for Chronos

For questions and discussions around Chronos, please use the Google Group "chronos-scheduler":
[Chronos Scheduler Group](https://groups.google.com/forum/#!forum/chronos-scheduler).

Also join us on IRC in #mesos on freenode.

## Reporting Bugs

Bugs can be reported by creating a [new GitHub issue](https://github.com/mesos/chronos/issues/new).

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

If the bug is in building Mesos from scratch, please [submit those bugs directly to the Apache Mesos JIRA](https://issues.apache.org/jira/browse/MESOS).

If the bug occurs while running Chronos, please include the following
information:

* The command used to launch Chronos, for example:

        java -cp target/chronos.jar org.apache.mesos.chronos.scheduler.Main <args>

* The version of Mesos you are running.

* The output of

        java -version