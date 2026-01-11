#!/bin/bash
set -e

# -------------------------------
# Base OS setup
# -------------------------------
dnf update -y
dnf install -y \
  java-21-amazon-corretto \
  java-21-amazon-corretto-devel \
  git \
  maven

# -------------------------------
# Set Java 21 explicitly (NO alternatives)
# -------------------------------
cat <<EOF >/etc/profile.d/java21.sh
export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
export PATH=\$JAVA_HOME/bin:\$PATH
EOF

source /etc/profile.d/java21.sh

# -------------------------------
# Persistent directories
# -------------------------------
mkdir -p /home/ec2-user/{h2,uploads,logs}
mkdir -p /opt/apps
chown -R ec2-user:ec2-user /home/ec2-user /opt/apps
chmod -R 755 /home/ec2-user

# -------------------------------
# Clone & build as ec2-user
# -------------------------------
su - ec2-user <<'EOF'
set -e
source /etc/profile.d/java21.sh

cd ~

git clone https://github.com/justamitsaha/aws-apps.git || true
cd aws-apps

cd fileReader
mvn clean package -DskipTests
cp target/fileReader-*.jar /home/ec2-user/apps/fileReader.jar

cd ../reporting
mvn clean package -DskipTests
cp target/reporting-*.jar /home/ec2-user/apps/reporting.jar
EOF

# -------------------------------
# Setup systemd services
# -------------------------------
bash /home/ec2-user/aws-apps/_documentation/1_ec2/setup-services.sh
