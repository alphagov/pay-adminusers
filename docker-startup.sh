#!/usr/bin/env bash

java -jar *-allinone.jar waitOnDependencies *.yaml && \
java -jar *-allinone.jar migrateToInitialDbState *.yaml && \
java -jar *-allinone.jar db migrate *.yaml && \
java -jar *-allinone.jar server *.yaml
