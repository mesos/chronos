#!/bin/sh

export LIBPROCESS_PORT="${PORT1}" 
exec java $JVM_OPTS -jar /chronos/chronos.jar $@ --http_port $PORT0
