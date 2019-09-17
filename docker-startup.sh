#!/usr/bin/env bash

set -eu

: "${RUN_MIGRATION:=false}"
: "${RUN_APP:=true}"

java $JAVA_OPTS -jar *-allinone.jar waitOnDependencies *.yaml

if [ "$RUN_MIGRATION" == "true" ]; then
  java $JAVA_OPTS -jar *-allinone.jar migrateToInitialDbState *.yaml
  java $JAVA_OPTS -jar *-allinone.jar db migrate *.yaml
fi

if [ "$RUN_APP" == "true" ]; then
  exec java $JAVA_OPTS -jar *-allinone.jar server *.yaml
fi
