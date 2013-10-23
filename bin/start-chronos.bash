#!/bin/bash
#
# This is a sample script for starting chronos. If you deploy the service you may want to build custom
# runit, monit or upstart scripts.
CHRONOS_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

echo $CHRONOS_HOME

# This setup assumes you started with the mesos source and installed the binaries into
# the ./build directory of the mesos source. Modify this as needed.

# TODO(FL): ensure this script runs on *nix as well.
# TODO(FL): clean-up!
echo "This script is setup to run on MacOSX right now. Modify it to run on other systems."
default_mesos_home=/usr/local/mesos
MESOS_HOME=${1:-$default_mesos_home}
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
java -cp "$CHRONOS_HOME"/target/chronos*.jar com.airbnb.scheduler.Main $@
