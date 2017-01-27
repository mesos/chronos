#!/bin/sh

if [ -d "$MESOS_SANDBOX/.ssl" ]; then
  cp -r $MESOS_SANDBOX/.ssl ./
fi

export LIBPROCESS_PORT="${PORT1}"
exec java $JVM_OPTS -jar /chronos/chronos.jar $@ --http_port $PORT0
