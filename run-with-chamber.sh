#!/usr/bin/env bash

AWS_REGION="${ECS_AWS_REGION}" chamber exec "${ECS_SERVICE}" -- ./docker-startup.sh
