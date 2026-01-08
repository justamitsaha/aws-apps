#!/bin/bash
set -e

REGION="us-east-1"
TAG_NAME="aws-app"

INSTANCE_ID=$(aws ec2 describe-instances \
  --region $REGION \
  --filters "Name=tag:Name,Values=$TAG_NAME" "Name=instance-state-name,Values=running,stopped,pending" \
  --query "Reservations[].Instances[].InstanceId" \
  --output text)

if [ -z "$INSTANCE_ID" ]; then
  echo "No instance found with tag Name=$TAG_NAME"
  exit 0
fi

echo "Terminating instance: $INSTANCE_ID"

aws ec2 terminate-instances \
  --region $REGION \
  --instance-ids "$INSTANCE_ID"

aws ec2 wait instance-terminated \
  --region $REGION \
  --instance-ids "$INSTANCE_ID"

echo "Instance terminated successfully"
