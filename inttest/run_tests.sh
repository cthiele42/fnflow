#!/bin/bash

_help() {
  echo "Run tests"
  echo -e "Usage: run_tests [-i] "
  exit 0
}

while getopts "i" options; do
  case "${options}" in
    i)
      INTERACTIVE=true
      ;;
    *)
      _help
      ;;
  esac
done

set -e

# initialize a pristine platform
cd ../deploy

# kind delete cluster
# kind create cluster --config kind-clusterconfig.yml

helmfile -e local apply

# run the tests
cd ../inttest
docker build --tag 'cypresscucumber' cypress-cucumber

if [ "$INTERACTIVE" == "true" ]; then
  xhost + local:
  docker run -it --rm --network host -e DISPLAY -v .:/app -w /app -u $(id -u) --entrypoint cypress cypresscucumber:latest open --project .
else
  docker run -it --rm --network host -v .:/app -w /app -u $(id -u) cypresscucumber:latest
fi

