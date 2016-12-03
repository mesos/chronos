---
title: Frequently Asked Questions
---


# Frequently Asked Questions


* [How do I find which Chronos node to talk to?](#which-node)
* [How does Chronos use ZooKeeper?](#chronos-zookeeper)
* [How does Chronos use Cassandra?](#chronos-cassandra)
* [[osx] making Mesos fails on `warning: 'JNI_CreateJavaVM' is deprecated`](#osx-mesos)
* [Running Chronos fails with the error: java.lang.UnsatisfiedLinkError: org.apache.mesos.state.AbstractState.__fetch(Ljava/lang/String;)J](#running-chronos)
* [My Web UI is not showing up!](#web-ui)
* [When running jobs locally I get an error like `Failed to execute 'chown -R'`](#running-jobs-locally)
* [I found a bug!](#bug)

### <a name="which-node"></a>How do I find which Chronos node to talk to?

Chronos is designed (not required) to run with multiple nodes of which one is elected master.
If you use the cURL command line tool, you can use the `-L` flag and hit any Chronos node and you will get a
307 REDIRECT to the leader.


### <a name="chronos-zookeeper"></a>How does Chronos use ZooKeeper?

Chronos registers itself with [ZooKeeper][ZooKeeper] at the location `/chronos/state`. This value can be changed via the configuration file.

### <a name="chronos-cassandra"></a>How does Chronos use Cassandra?

Chronos can optionally use [Cassandra] for job history, reporting and statistics. By default, Chronos attempts to connect to the `metrics` keyspace.
To use this feature, you must at a minimum:

1. Create a keyspace (named `metrics` and configurable with `--cassandra_keyspace`)
```sql
CREATE KEYSPACE IF NOT EXISTS metrics
WITH REPLICATION = {
  'class' : 'SimpleStrategy', 'replication_factor' : 3
};
```
1. Pass the `--cassandra_contact_points` flag to Chronos with a comma-separated list of Cassandra contact points

### <a name="osx-mesos"></a>[osx] Making Mesos fails on deprecated header warning

Error message such as:

    conftest.cpp:7: warning: 'JNI_CreateJavaVM' is deprecated (declared at /System/Library/Frameworks/JavaVM.framework/Headers/jni.h:1937)

This error is the result of OSX shipping with an outdated version of the JDK and associated libraries. To resolve this issue, do the following.

1. [Download](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and install JDK7.
2. Set JDK7 as active:  
`export JAVA_HOME=$(/usr/libexec/java_home -v 1.7)`  
**Note:** Stick this in your `~/.*rc` to always use 1.7
3. Find your JNI headers, these should be in `$JAVA_HOME/include` and `$JAVA_HOME/include/darwin`.
4. Configure mesos with `JAVA_CPPFLAGS` set to the JNI path.

**Example Assumptions:**  

* `$JAVA_HOME` in this example is `/Library/Java/JavaVirtualMachines/jdk1.7.0_12.jdk/Contents/Home`
* The current working directory is `mesos/build` as advised by the [mesos README](https://github.com/apache/mesos/blob/trunk/README#L13)  

**Example:**

    JAVA_CPPFLAGS='-I/Library/Java/JavaVirtualMachines/jdk1.7.0_12.jdk/Contents/Home/include/ -I/Library/Java/JavaVirtualMachines/jdk1.7.0_12.jdk/Contents/Home/include/darwin/' ../configure

### <a name="running-chronos"></a>Running Chronos fails with the error: `java.lang.UnsatisfiedLinkError: org.apache.mesos.state.AbstractState.__fetch(Ljava/lang/String;)J`

This means you're using a mesos-jar file that is incompatible with the version of Mesos you're running.
If you want to run chronos with a different version of mesos than in the pom.xml file, override the version by issuing `mvn package -Dmesos.version=0.14.0-rc4`.
Please note, this must be a jar file version that's available from one of the repositories listed in the pom.xml file.


### <a name="web-ui"></a>My Web UI is not showing up!

For asset bundling, you need node installed. If you're seeing a 403 when trying to access the web-ui, it's likely that node was not present during the `mvn package` step.

See [docs/webui.md](http://mesos.github.io/chronos/docs/webui.html).

### <a name="running-jobs-locally"></a>When running jobs locally I get an error like `Failed to execute 'chown -R'`

If you get an error such as:

		Failed to execute 'chown -R 0:0 '/tmp/mesos/slaves/executors/...' ... Undefined error: 0
		Failed to launch executor`

You can try starting your mesos slaves with switch users disabled. To do this, start your slaves in the following manner:  

		MESOS_SWITCH_USER=0 bin/mesos-slave.sh --master=zk://localhost:2181/mesos --resources="cpus:8,mem:68551;disk:803394"

### <a name="bug"></a>I found a bug!

Please see the [support page](http://mesos.github.io/chronos/support.html) for information on how to report bugs.

[ZooKeeper]: https://zookeeper.apache.org/
[Cassandra]: http://cassandra.apache.org
