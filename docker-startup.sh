#!/usr/bin/env bash

set -eu
RUN_MIGRATION=${RUN_MIGRATION:-false}
RUN_APP=${RUN_APP:-true}

if [ -n "${CERTS_PATH:-}" ]; then
  i=0
  truststore=$JAVA_HOME/lib/security/cacerts
  truststore_pass=changeit
  for cert in "$CERTS_PATH"/*; do
    [ -f "$cert" ] || continue
    echo "Adding $cert to $truststore"
    keytool -importcert -noprompt -keystore "$truststore" -storepass "$truststore_pass" -file "$cert" -alias custom$((i++))
  done
fi

java $JAVA_OPTS -jar *-allinone.jar waitOnDependencies *.yaml

if [ "$RUN_MIGRATION" == "true" ]; then
  java $JAVA_OPTS -jar *-allinone.jar migrateToInitialDbState *.yaml
  java $JAVA_OPTS -jar *-allinone.jar db migrate *.yaml
fi

if [ "$RUN_APP" == "true" ]; then
  java $JAVA_OPTS -jar *-allinone.jar server *.yaml
fi

exit 0
