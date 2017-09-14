#!/bin/bash

DOCKER_IMAGE_NAME=fullstack-twilio-demo
set -ex

# prune unused containers
docker container prune -f
# build test app image
docker build -t $DOCKER_IMAGE_NAME .
# run image
docker run -p 4567:4567 \
    --name $DOCKER_IMAGE_NAME-webserver \
    --env-file settings.env \
    $DOCKER_IMAGE_NAME \
