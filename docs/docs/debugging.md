---
title: Debugging
---

### Debugging Chronos

Chronos uses log4j to control log output.  To override the standard log4j configuration,
create a [log4j configuration file](http://logging.apache.org/log4j/1.2/manual.html) and
add `-Dlog4j.configuration=file:<path to config>` to the Chronos startup command.

### Debugging Individual Jobs
Individual jobs log with their task id on the mesos slaves.
Look in the standard out log for your job name and the string "ready for launch", or else "job ct:" and your job name.
The job is done when the line in the log says:

    Task with id 'value: TASK_ID **FINISHED**

To find debug logs on the mesos slave, look in `/tmp/mesos/slaves` on the slave instance (unless you've specifically supplied a different log folder for mesos). For example:

    /tmp/mesos/slaves/

In that dir, the current slave run is timestamped so look for the most recent.
Under that is a list of frameworks; you're interested in the Chronos framework.
For example:

    /tmp/mesos/slaves/&lt;timestamp&gt;/frameworks/