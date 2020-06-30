#!/bin/bash

VERSION=v0.0.1
IMG=411026478373.dkr.ecr.us-east-1.amazonaws.com/influxdb-data-backup:${VERSION}
docker build -t $IMG .
docker-ecr-login
docker push $IMG
