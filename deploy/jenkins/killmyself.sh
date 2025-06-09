#!/bin/bash

function kill_my_self() {
  TIMESTAMP=$(date +%s)
  INSTANCE_ID=$( curl http://169.254.169.254/hetzner/v1/metadata/instance-id )

  OLD_IMAGE_ID=$( curl -s \
      -H "Authorization: Bearer ${CLOUD_API_TOKEN}" \
      'https://api.hetzner.cloud/v1/images?type=snapshot&label_selector=buildserver' \
      | jq -r '.images[0].id' )

  IMAGE_ID=$( curl -s -XPOST \
    -H "Authorization: Bearer ${CLOUD_API_TOKEN}" \
    -H 'Content-Type: application/json' \
    'https://api.hetzner.cloud/v1/servers/'${INSTANCE_ID}'/actions/create_image' \
    -d '{
      "description": "fnflow-buildserver-'"${TIMESTAMP}"'",
      "type": "snapshot",
      "labels": {
          "buildserver": ""
      }
    }' | jq -r '.image.id')

  TS=$( date +"%Y-%m-%dT%H:%M:%S%z" )
  echo "$TS  Saving the snapshot ..." >> /var/log/killmyself.log
  while :
  do
    sleep 10s
    IMAGE_STATUS=$( curl -sH "Authorization: Bearer $CLOUD_API_TOKEN" "https://api.hetzner.cloud/v1/images/${IMAGE_ID}" | jq -r '.image.status' )
    if [ "$IMAGE_STATUS" = "available" ]; then
      break;
    fi
    TS=$( date +"%Y-%m-%dT%H:%M:%S%z" )
    echo "$TS  Still saving ..." >> /var/log/killmyself.log
  done
  TS=$( date +"%Y-%m-%dT%H:%M:%S%z" )
  echo "$TS  Snapshot saved ..." >> /var/log/killmyself.log

  curl -X DELETE -H "Authorization: Bearer $CLOUD_API_TOKEN" "https://api.hetzner.cloud/v1/images/$OLD_IMAGE_ID"

  TS=$( date +"%Y-%m-%dT%H:%M:%S%z" )
  echo "$TS  Killing myself. Good Bye! ..." >> /var/log/killmyself.log
  curl -X DELETE -H "Authorization: Bearer $CLOUD_API_TOKEN" "https://api.hetzner.cloud/v1/servers/$INSTANCE_ID"
}


count=0

while IFS= read -r number; do
    if (( $(echo "$number <  0.1" |bc -l) )); then
      ((count++))
    else
      break;
    fi
done < <(curl -sL 'http://localhost:8080/computer/api/json?depth=3' | jq '.computer[0].loadStatistics.busyExecutors.sec10.history[]')

TS=$( date +"%Y-%m-%dT%H:%M:%S%z" )

if [ "$count" -gt "60" ]; then
  echo "$TS  Jenkins is more than 10 minutes idle -  start killing myself ..." >> /var/log/killmyself.log
  kill_my_self
else
  echo "$TS  Jenkins seems to be busy recently ..." >> /var/log/killmyself.log
fi
