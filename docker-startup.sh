#!/usr/bin/env bash

java -jar *-allinone.jar waitOnDependencies *.yaml && \
java -jar *-allinone.jar migrateToInitialDbState *.yaml && \
java -jar *-allinone.jar db migrate *.yaml && \

if [ -n "$CHAMBER" ]; then
  AWS_REGION=${ECS_AWS_REGION} chamber exec ${ECS_SERVICE} -- java $JAVA_OPTS -jar *-allinone.jar server *.yaml
else
  java $JAVA_OPTS -jar *-allinone.jar server *.yaml
fi
