#!/bin/bash
set -e

echo "Setting up systemd services..."

# ---------- fileReader ----------
cat <<EOF >/etc/systemd/system/fileReader.service
[Unit]
Description=FileReader Spring Boot Service
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user/apps
ExecStart=/usr/bin/java -Dspring.profiles.active=ec2 -jar /opt/apps/fileReader.jar
SuccessExitStatus=143
Restart=always
RestartSec=5
Environment=JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
Environment=PATH=/usr/lib/jvm/java-21-amazon-corretto/bin:/usr/bin

[Install]
WantedBy=multi-user.target
EOF

# ---------- reporting ----------
cat <<EOF >/etc/systemd/system/reporting.service
[Unit]
Description=Reporting Spring Boot Service
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user/apps
ExecStart=/usr/bin/java -Dspring.profiles.active=ec2 -Dserver.port=8081 -jar /opt/apps/reporting.jar
SuccessExitStatus=143
Restart=always
RestartSec=5
Environment=JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
Environment=PATH=/usr/lib/jvm/java-21-amazon-corretto/bin:/usr/bin

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable fileReader reporting
systemctl start fileReader reporting

echo "systemd services installed and started"
