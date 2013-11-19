# Chronos FAQ

## Table of Contents

* [[osx] making mesos fails on `warning: 'JNI_CreateJavaVM' is deprecated`](#osx-making-mesos-fails-on-deprecated-warning)
* [Running Chronos fails with the error: `java.lang.UnsatisfiedLinkError: org.apache.mesos.state.AbstractState.__fetch(Ljava/lang/String;)J`](#mesos-versioning-problem)
* [My Web UI is not showing up!](#my-web-ui-is-not-showing-up)
* [When running jobs locally I get an error like `Failed to execute 'chown -R'`](#when-running-jobs-locally-i-get-an-error-like-failed-to-execute-chown--r)
* [I found a bug!](#i-found-a-bug)

## [osx] Making mesos fails on deprecated header warning

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

## Running Chronos fails with the error: `java.lang.UnsatisfiedLinkError: org.apache.mesos.state.AbstractState.__fetch(Ljava/lang/String;)J`

This means you're using a mesos-jar file that is incompatible with the version of Mesos you're running.
If you want to run chronos with a different version of mesos than in the pom.xml file, override the version by issuing `mvn package -Dmesos.version=0.14.0-rc4`.
Please note, this must be a jar file version that's available from one of the repositories listed in the pom.xml file.


## My Web UI is not showing up!

See [docs/WEBUI.md](/docs/WEBUI.md).

## When running jobs locally I get an error like `Failed to execute 'chown -R'`

If you get an error such as:

		Failed to execute 'chown -R 0:0 '/tmp/mesos/slaves/executors/...' ... Undefined error: 0
		Failed to launch executor`

You can try starting your mesos slaves with switch users disabled. To do this, start your slaves in the following manner:  

		MESOS_SWITCH_USER=0 bin/mesos-slave.sh --master=zk://localhost:2181/mesos --resources="cpus:8,mem:68551;disk:803394"

## I found a bug!

That's too bad, please start by consulting [our guidelines on Reporting
Bugs](/airbnb/chronos#reporting-bugs).
