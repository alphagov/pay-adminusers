#!/bin/bash

set -e

cd "$(dirname "$0")"

mvn -DskipTests clean package
docker build -t govukpay/adminusers:local .
