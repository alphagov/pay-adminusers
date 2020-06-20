#!/usr/bin/env bash

set -eu

: "${RUN_MIGRATION:=false}"
: "${RUN_APP:=true}"

# shellcheck disable=SC2086
java $JAVA_OPTS -jar ./*-allinone.jar waitOnDependencies ./*.yaml

if [ "$RUN_MIGRATION" == "true" ]; then
  # shellcheck disable=SC2086
  java $JAVA_OPTS -jar ./*-allinone.jar db migrate ./*.yaml
fi

if [ "$RUN_APP" == "true" ]; then
  # shellcheck disable=SC2086
  exec java $JAVA_OPTS -jar ./*-allinone.jar server ./*.yaml
fi
