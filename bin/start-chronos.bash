#!/bin/bash
set -o errexit -o nounset -o pipefail

# allow overriding ENTRYPOINT
# see https://github.com/docker-library/official-images/issues/692
if [ "${1:-absent}" == "absent" ] || [ "${1:0:1}" == '-' ]; then
  flags=( --http_port 8080 )
  flags+=( "$@" )

  heap=384m

  #If we're on Amazon, let's use the public hostname so redirect works as expected.
  if public_hostname="$( curl -sSf --connect-timeout 1 http://169.254.169.254/latest/meta-data/public-hostname 2>/dev/null)"
  then
    flags+=( --hostname $public_hostname )
  else
    flags+=( --hostname `hostname` )
  fi

  set -- java -Xmx"$heap" -Xms"$heap" -cp "/chronos.jar" \
     org.apache.mesos.chronos.scheduler.Main \
     "${flags[@]}"
fi

exec "$@"
