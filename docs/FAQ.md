# Chronos FAQ
## Table of Contents
1. [[osx] making mesos fails on `warning: 'JNI_CreateJavaVM' is deprecated`](#osx-making-mesos-fails-on-deprecated-warning)
2. [My Web UI is not showing up!](#my-web-ui-is-not-showing-up)
3. [When running jobs locally I get an error like `Failed to execute 'chown -R'`](#when-running-jobs-locally-i-get-an-error-like-failed-to-execute-chown--r)


## [osx] Making mesos fails on deprecated header warning
Error message such as:
`conftest.cpp:7: warning: 'JNI_CreateJavaVM' is deprecated (declared at /System/Library/Frameworks/JavaVM.framework/Headers/jni.h:1937)`

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
`JAVA_CPPFLAGS='-I/Library/Java/JavaVirtualMachines/jdk1.7.0_12.jdk/Contents/Home/include/ -I/Library/Java/JavaVirtualMachines/jdk1.7.0_12.jdk/Contents/Home/include/darwin/' ../configure`

## My Web UI is not showing up!
See [docs/WEBUI.md](/docs/WEBUI.md).

## When running jobs locally I get an error like `Failed to execute 'chown -R'`

If you get an error such as:

		Failed to execute 'chown -R 0:0 '/tmp/mesos/slaves/executors/...' ... Undefined error: 0
		Failed to launch executor`

You can try starting your mesos slaves with switch users disabled. To do this, start your slaves in the following manner:  

		MESOS_SWITCH_USER=0 bin/mesos-slave.sh --master=zk://localhost:2181/mesos --resources="cpus:8,mem:68551;disk:803394"