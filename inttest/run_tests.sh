#!/bin/bash

# initialize a pristine platform
cd ../deploy
kind delete cluster
kind create cluster --config kind-clusterconfig.yml
helmfile -e local apply

# run the tests
cd ../inttest
docker build --tag 'cypresscucumber' cypress-cucumber
xhost + local:
docker run -it --rm --network host -e DISPLAY -v .:/app -w /app -u $(id -u) --entrypoint cypress cypresscucumber:latest open --project .
