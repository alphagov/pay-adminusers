#!/bin/bash

# The cert bundles distributed by AWS are bundles which contain multiple CA cert
# chains. The keytool command can only import a single cert/chain, and will 
# silently import the first and ignore the rest. So we need to break the 
# bundle up into individual certs and then import them individually.
#
# This file was heavily based on the AWS example https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.SSL-certificate-rotation.html#UsingWithRDS.SSL-certificate-rotation-sample-script

set -euo pipefail

TMPDIR=$(mktemp -d)

for REGION in eu-west-1 eu-central-1; do
  mkdir "${TMPDIR}/${REGION}"

  wget -q "https://truststore.pki.rds.amazonaws.com/${REGION}/${REGION}-bundle.pem" -O ${TMPDIR}/${REGION}-bundle.pem
  awk 'BEGIN { n=0 } split_after == 1 {n++;split_after=0} /-----END CERTIFICATE-----/ {split_after=1}{print > "'${TMPDIR}/${REGION}'/rds-'"${REGION}"'-ca-" n ".pem"}' < ${TMPDIR}/${REGION}-bundle.pem

  find "${TMPDIR}/${REGION}" -name '*.pem' | while read -r CERT; do
    echo "Importing $CERT"
    keytool -importcert -noprompt -cacerts -storepass changeit -alias "${CERT}" -file "${CERT}"
    rm "$CERT"
  done

  rm "${TMPDIR}/${REGION}-bundle.pem"
done

echo "removing TMPDIR"
rm -rf "${TMPDIR}"
