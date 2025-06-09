#!/bin/bash

# CLOUD_API_TOKEN has to be set

IMAGE_ID=$( curl -s \
  -H "Authorization: Bearer ${CLOUD_API_TOKEN}" \
  'https://api.hetzner.cloud/v1/images?type=snapshot&label_selector=buildserver' \
  | jq -r '.images[0].id' )

SERVER_ID=$( curl -s -XPOST \
  -H "Authorization: Bearer ${CLOUD_API_TOKEN}" \
  -H 'Content-Type: application/json' \
  'https://api.hetzner.cloud/v1/servers' \
  -d '{
    "name": "fnflow-buildserver",
    "location": "fsn1",
    "server_type": "cpx51",
    "image": "'"${IMAGE_ID}"'",
    "ssh_keys": [
      "cthiele@ct42.de"
    ],
    "labels": {
        "type": "buildserver"
    },
    "public_net": {
      "ipv4": 88962176,
      "ipv6": 88962177
    }
  }' | jq -r '.server.id' )

echo "Waiting for the server to be running ..."
while :
do
  sleep 30s
  SERVER_STATUS=$( curl -sH "Authorization: Bearer $CLOUD_API_TOKEN" "https://api.hetzner.cloud/v1/servers/$SERVER_ID" | jq -r '.server.status' )
  if [ "$SERVER_STATUS" = "running" ]; then
    break;
  fi
done
echo "The server is running now and reachable via http://10.9.0.1:8080. Happy building!"
