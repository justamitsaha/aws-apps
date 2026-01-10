#!/bin/bash
set -e

BUCKET="amit-app-artifacts"

echo "Building fileReader..."
cd ../../fileReader
mvn clean package -DskipTests
aws s3 cp target/fileReader-*.jar s3://$BUCKET/fileReader/fileReader.jar

echo "Building reporting..."
cd ../reporting
mvn clean package -DskipTests
aws s3 cp target/reporting-*.jar s3://$BUCKET/reporting/reporting.jar

echo "Artifacts uploaded to S3"
