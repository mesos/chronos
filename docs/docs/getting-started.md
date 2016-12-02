---
title: Setting Up and Running Chronos
---

# Setting up and Running Chronos

## Quickstart

The [dcos-vagrant](https://github.com/dcos/dcos-vagrant) provides an easy way to try DC/OS and Chronos within a virtual machine using Vagrant.

## Requirements

These requirements are just to run Chronos. You will need additional packages to build Chronos from source (see the [Building from Source section](#build-from-source) below).

* [Apache Mesos][Mesos] 1.0.0+
* [Apache ZooKeeper][ZooKeeper]
* JDK 1.8+


## Install from Packages

Mesosphere provides Docker images for Chronos, available from Docker hub at <https://hub.docker.com/r/mesosphere/chronos/>.

## <a name="build-from-source"></a>Building from Source

Follow these steps to build Chronos from source. This configuration assumes you already have Mesos installed on the same host (see Mesosphere link above to get a Mesos package).

## Requirements

These requirements are to build and run Chronos.

* [Apache Mesos][Mesos] 1.0.0+
* [Apache ZooKeeper][ZooKeeper]
* JDK 1.8+
* [Maven 3+](https://maven.apache.org/download.cgi)
* NodeJS 7+


### Build Chronos

Install [Node](http://nodejs.org/) first. On OSX, try `brew install node`.

Start up Zookeeper, Mesos master, and Mesos slave(s).  Then try

    export MESOS_NATIVE_LIBRARY=/usr/local/lib/libmesos.so
    git clone https://github.com/mesos/chronos.git
    cd chronos
    mvn package
    java -cp target/chronos*.jar org.apache.mesos.chronos.scheduler.Main --master zk://localhost:2181/mesos --zk_hosts localhost:2181

### Environment Variables Mesos Looks For

* `MESOS_NATIVE_LIBRARY`: Absolute path to the native mesos library. This is usually `/usr/local/lib/libmesos.so` on Linux and `/usr/local/lib/libmesos.dylib` on OSX.

If you're using the installer script this should be setup for you.

<hr />

## Running Chronos

The basic syntax for launching chronos is:

    java -jar chronos.jar --master zk://127.0.0.1:2181/mesos --zk_hosts 127.0.0.1:2181

Please note that you need to have both Mesos and Zookeeper running for this to work!

For more information on configuration options, please see [Configuration]({{ site.baseurl }}/docs/configuration.html).

[Mesos]: https://mesos.apache.org/ "Apache Mesos"
[Zookeeper]: https://zookeeper.apache.org/ "Apache ZooKeeper"
