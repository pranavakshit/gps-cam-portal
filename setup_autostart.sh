#!/bin/bash

echo "=========================================================="
echo " Setting up Auto-Start for GPS Cam Portal & Domains"
echo "=========================================================="

# 1. Enable Docker service to start on boot
echo "[1/3] Enabling Docker service to start on boot..."
sudo systemctl enable docker
sudo systemctl enable containerd

# 2. Restart all docker-compose containers to apply the new 'restart: always' policy
echo "[2/3] Restarting containers to apply the 'restart: always' policy..."
cd /opt/gps-cam-portal
docker-compose down
docker-compose up -d

echo "[3/3] Done! Whenever your Ubuntu VM is rebooted or powered on,"
echo "      Docker will automatically start and spin up your backend,"
echo "      web portal, and database automatically."
echo "=========================================================="
