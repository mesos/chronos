#!/bin/bash
set -o errexit -o nounset -o pipefail

chronos_home="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd -P )"
echo "Chronos home set to $chronos_home"
export JAVA_LIBRARY_PATH="/usr/local/lib:/lib:/usr/lib"
export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:-/lib}"
export LD_LIBRARY_PATH="$JAVA_LIBRARY_PATH:$LD_LIBRARY_PATH"

flags=( "$@" )

#If we're on Amazon, let's use the public hostname so redirect works as expected.
if public_hostname="$( curl -sSf --connect-timeout 1 http://169.254.169.254/latest/meta-data/public-hostname )"
then
  flags+=( --hostname $public_hostname )
else
  flags+=( --hostname `hostname` )
fi

jar_files=( "$chronos_home"/target/chronos*.jar )
echo "Using jar file: $jar_files[0]"

heap=384m

java -Xmx"$heap" -Xms"$heap" -cp "${jar_files[0]}" \
     com.airbnb.scheduler.Main \
     "${flags[@]}"
