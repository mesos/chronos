#!/bin/sh

LIBPROCESS_PORT="${PORT1}" java $JVM_OPTS -jar /chronos/chronos.jar $@ --http_port $PORT0
