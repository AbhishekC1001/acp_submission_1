#!/bin/bash

awslocal s3 mb s3://s2891348


awslocal dynamodb create-table \
    --table-name s2891348 \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

echo "********** LOCALSTACK INITIALIZED **********"