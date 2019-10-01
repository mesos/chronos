#!/bin/bash
set -o errexit -o nounset -o pipefail

# allow overriding ENTRYPOINT
# see https://github.com/docker-library/official-images/issues/692
if [ "${1:-absent}" == "absent" ] || [ "${1:0:1}" == '-' ]; then
  flags=( "$@" )

  heap=384m
  chronos_home="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd -P )"
  echo "Chronos home set to $chronos_home"
  export JAVA_LIBRARY_PATH="/usr/local/lib:/lib:/usr/lib"
  export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:-/lib}"
  export LD_LIBRARY_PATH="$JAVA_LIBRARY_PATH:$LD_LIBRARY_PATH"


  #If we're on Amazon, let's use the public hostname so redirect works as expected.
  if public_hostname="$( curl -sSf --connect-timeout 1 http://169.254.169.254/latest/meta-data/public-hostname 2>/dev/null)"
  then
    flags+=( --hostname $public_hostname )
  else
    flags+=( --hostname `hostname` )
  fi

  jar_files=( "$chronos_home"/target/chronos*.jar )
  echo "Using jar file: $jar_files[0]"

  # start zookeeper if we are inside docker container
  if test -f /.dockerinit; then
    echo "Starting Zookeeer.."
    service zookeeper start
  fi
  set -- java -Xmx"$heap" -Xms"$heap" -cp "${jar_files[0]}" -Dlog4j.configuration=file:$chronos_home/log4j.properties \
     org.apache.mesos.chronos.scheduler.Main \
     "${flags[@]}"
fi

exec "$@"
