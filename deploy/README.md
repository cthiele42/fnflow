# Test Deployment
This module provides a test deplyoment for the `fnflow-json-processors-kafka` application.
It is based on a kubernetes deployment using [kind](https://kind.sigs.k8s.io/).  
A kind cluster config is provided exposing ports to opensearch, opensearch dashboards and akhq on the host machine.  
The cluster can be created with the following command:
```shell
kind create cluster --config kind-clusterconfig.yml
```
The exposed adresses are:
- Opensearch: [http://locahost:9200](http://locahost:9200)
- Opensearch Dashboards: [http://locahost:5601](http://locahost:5601)
- AKHQ: [http://locahost:32551](http://locahost:32551)
- KafkaService: [http://localhost:32580](http://localhost:32580)

The deployment is made with [helmfile](https://github.com/helmfile/helmfile).  
Following will be deployed:
- Kafka
- AKHQ
- Opensearch
- Opensearch Dashboards
- fnflow-json-processors-kafka application
- fnflow-kafkaservice

The functions can be configured in the [values.yaml](local/values.yaml)

Example:
```yaml
    cfgfns:
      hasValueValidator:
        idExist:
          elementPath: /id
      Match:
        idMatch:
          index: testindex
          template: testtemplate
          paramsFromInput:
            ids: /id
          literalParams:
            field: id
```
Deploying the test system can be done with the following command:
```shell
helmfile -e local apply
```
Redeploying the fnflow app after a config change can be done this way:
```shell
helmfile -e local -l name=fnflowprocessors apply
```
