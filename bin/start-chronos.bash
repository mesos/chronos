#!/bin/bash
#
# This is a sample script for starting chronos. If you deploy the service you may want to build custom
# runit, monit or upstart scripts.
CHRONOS_HOME=$(dirname $0)/..

# This setup assumes you started with the mesos source and installed the binaries into
# the ./build directory of the mesos source. Modify this as needed.

# TODO(FL): ensure this script runs on *nix as well.
# TODO(FL): clean-up!
echo "This script is setup to run on MacOSX right now. Modify it to run on other systems."
MESOS_HOME=/usr/local/mesos
echo "MESOS_HOME is set to: $MESOS_HOME"
pushd $MESOS_HOME
libmesos_file=$(find . -name "libmesos.dylib" | head -n1)
build_env=$(find . -name "mesos-build-env.sh" | head -n1)
export MESOS_NATIVE_LIBRARY="${MESOS_HOME}/${libmesos_file}"
echo "MESOS_NATIVE_LIBRARY set to $MESOS_NATIVE_LIBRARY"
echo "Sourcing mesos-build-env.sh: $build_env"
source $build_env
popd

# Start chronos.
java -cp "$CHRONOS_HOME"/target/chronos*.jar com.airbnb.scheduler.Main server "$CHRONOS_HOME"/config/local_scheduler_nozk.yml
