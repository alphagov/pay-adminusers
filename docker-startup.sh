#!/usr/bin/env bash

set -eu
RUN_MIGRATION=${RUN_MIGRATION:-false}
RUN_APP=${RUN_APP:-true}

java -jar *-allinone.jar waitOnDependencies *.yaml

if [ "$RUN_MIGRATION" == "true" ]; then
  java -jar *-allinone.jar migrateToInitialDbState *.yaml
  java -jar *-allinone.jar db migrate *.yaml
fi

if [ "$RUN_APP" == "true" ]; then
  java $JAVA_OPTS -jar *-allinone.jar server *.yaml
fi

exit 0
