#!/bin/bash
ENV_FILE="$WORKSPACE/pay-scripts/services/adminusers.env"
if [ -f "$ENV_FILE" ]
then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a  
fi

eval "$@"
