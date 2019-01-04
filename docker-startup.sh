#!/usr/bin/env bash

set -eu
RUN_MIGRATION=${RUN_MIGRATION:-false}
RUN_APP=${RUN_APP:-true}

[ -z "${http_proxy:-}" ] || JAVA_OPTS="${JAVA_OPTS:-} -Dhttp.proxyHost=${http_proxy}"
[ -z "${https_proxy:-}" ] || JAVA_OPTS="${JAVA_OPTS:-} -Dhttps.proxyHost=${https_proxy}"
[ -z "${java_http_non_proxy_hosts:-}" ] || JAVA_OPTS="${JAVA_OPTS:-} -Dhttp.nonProxyHosts=${java_http_non_proxy_hosts}"

java $JAVA_OPTS -jar *-allinone.jar waitOnDependencies *.yaml

if [ "$RUN_MIGRATION" == "true" ]; then
  java $JAVA_OPTS -jar *-allinone.jar migrateToInitialDbState *.yaml
  java $JAVA_OPTS -jar *-allinone.jar db migrate *.yaml
fi

if [ "$RUN_APP" == "true" ]; then
  java $JAVA_OPTS -jar *-allinone.jar server *.yaml
fi

exit 0
