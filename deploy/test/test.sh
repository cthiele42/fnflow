#!/bin/bash

set -euo pipefail

function setup() {
  # create searchtemplate
  curl -f -H 'Content-Type: application/json' -XPOST localhost:9200/_scripts/testtemplate -d '
  {
    "script": {
      "lang": "mustache",
      "source": "{\"query\":{\"terms\":{\"{{field}}\":{{#toJson}}ids{{/toJson}}}}}"
    }
  }
  '
  echo

  # create index with keyword mapping for id
  curl -f -H 'Content-Type: application/json' -XPUT localhost:9200/testindex -d '
  {
    "mappings": {
      "properties": {
        "id": {
          "type": "keyword"
        }
      }
    }
  }
  '
  echo

  #index two documents
  curl -f -H 'Content-Type: application/json' -XPUT localhost:9200/testindex/_doc/doc1 -d '
  {
    "id": ["ID1", "ID2", "ID3"]
  }'
  echo

  curl -f -H 'Content-Type: application/json' -XPUT localhost:9200/testindex/_doc/doc2 -d '
  {
    "id": "ID4"
  }'
  echo
}

setup

MSGS=''
for i in $(seq 0 999);
do
  if ((i % 3 == 0)); then
    MSGS=$MSGS'|{\"id\":[]}\n'
  else
    MSGS=$MSGS'|{\"id\":[\"T'$i'\"]}\n'
  fi
done

curl -sf -H 'Content-Type: application/json' -XPOST http://localhost:32551/api/local/topic/fnFlowComposedFnBean-in-0/data -d '
{
  "key": "",
  "value": "'${MSGS}'",
  "multiMessage": true,
  "keyValueSeparator":"|",
  "headers": []
}' > /dev/null
