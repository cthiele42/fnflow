#!/bin/bash

# initialize a pristine platform
cd ../deploy
kind delete cluster
kind create cluster --config kind-clusterconfig.yml
helmfile -e local apply

# run the tests
cd ../inttest
docker build --tag 'cypresscucumber' cypress-cucumber
docker run -it --rm --network host -v .:/app -w /app -u $(id -u) cypresscucumber:latest
