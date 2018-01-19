#!/usr/bin/env bash

AWS_REGION="${ECS_AWS_REGION}" ./chamber--linux-amd64 exec "${ECS_SERVICE}" -- ./docker-startup.sh
