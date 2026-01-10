#!/bin/bash
set -e

# -------------------------------
# CONFIG
# -------------------------------
REGION="us-east-1"
INSTANCE_TYPE="t3.micro"
KEY_NAME="anju"
SECURITY_GROUP_ID="sg-0f6f47c749ef0767b"
SUBNET_ID="subnet-0277585378ad5d2d8"
TAG_NAME="aws-app"
USER_DATA_FILE="./ec2_userdata.sh"
ROOT_VOLUME_SIZE=8   # GB
IAM_INSTANCE_PROFILE="EC2S3ReadRole"


# -------------------------------
# Find latest Amazon Linux 2023 AMI
# -------------------------------
AMI_ID=$(aws ec2 describe-images \
  --region $REGION \
  --owners amazon \
  --filters \
    "Name=name,Values=al2023-ami-*-x86_64" \
    "Name=state,Values=available" \
  --query "Images | sort_by(@, &CreationDate)[-1].ImageId" \
  --output text)

echo "Using AMI: $AMI_ID"

# -------------------------------
# Launch EC2 instance
# -------------------------------
INSTANCE_ID=$(MSYS_NO_PATHCONV=1 aws ec2 run-instances \
  --region $REGION \
  --image-id "$AMI_ID" \
  --instance-type "$INSTANCE_TYPE" \
  --key-name "$KEY_NAME" \
  --security-group-ids "$SECURITY_GROUP_ID" \
  --subnet-id "$SUBNET_ID" \
  --associate-public-ip-address \
  --block-device-mappings "DeviceName=/dev/xvda,Ebs={VolumeSize=$ROOT_VOLUME_SIZE,VolumeType=gp3,DeleteOnTermination=true}" \
  --iam-instance-profile Name=$IAM_INSTANCE_PROFILE \
  --user-data file://$USER_DATA_FILE \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$TAG_NAME}]" \
  --query "Instances[0].InstanceId" \
  --output text
)

echo "Instance launched: $INSTANCE_ID"

# -------------------------------
# Wait for running state
# -------------------------------
aws ec2 wait instance-running \
  --region $REGION \
  --instance-ids "$INSTANCE_ID"

# -------------------------------
# Fetch public IP
# -------------------------------
PUBLIC_IP=$(aws ec2 describe-instances \
  --region $REGION \
  --instance-ids "$INSTANCE_ID" \
  --query "Reservations[0].Instances[0].PublicIpAddress" \
  --output text)

echo "EC2 is running"
echo "Public IP: $PUBLIC_IP"
echo "SSH: ssh -i anju.pem ec2-user@$PUBLIC_IP"