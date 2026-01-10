#!/bin/bash
set -e

# -------------------------------
# OS + Java only
# -------------------------------
dnf update -y
dnf install -y java-21-amazon-corretto awscli

# -------------------------------
# Java env
# -------------------------------
cat <<EOF >/etc/profile.d/java21.sh
export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
export PATH=\$JAVA_HOME/bin:\$PATH
EOF

source /etc/profile.d/java21.sh

# -------------------------------
# Directories
# -------------------------------
mkdir -p /opt/apps /home/ec2-user/logs /home/ec2-user/h2
chown -R ec2-user:ec2-user /opt/apps /home/ec2-user

# -------------------------------
# Download artifacts from S3
# -------------------------------
aws s3 cp s3://amit-app-artifacts/fileReader/fileReader.jar /opt/apps/fileReader.jar
aws s3 cp s3://amit-app-artifacts/reporting/reporting.jar /opt/apps/reporting.jar

# -------------------------------
# Setup systemd services
# -------------------------------
bash /home/ec2-user/aws-apps/_documentation/ec2/setup-services.sh
