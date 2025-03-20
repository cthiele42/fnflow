# Processor Configuration
## Implementation
Using https://github.com/fabric8io/kubernetes-client

For a processor, only a deployment is needed, a service can be left out. Later, the metrics can be kept from the pod itself using [PodMonitor](https://prometheus-operator.dev/docs/developer/getting-started/#using-podmonitors).

## Writing in Batches and Compression
```yaml
spring:
  cloud:
    stream:
      kafka:
        default:
          producer:
            compression-type: lz4
            configuration:
              batch.size: 131072
              linger.ms: 50
```
## App Props
```yaml
spring.cloud.stream.default.group: ${pipeline-name}
spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination: ${source-topic}
spring.cloud.stream.bindings.fnFlowComposedFnBean-out-0.destination: ${out-topic}
spring.cloud.stream.bindings.fnFlowComposedFnBean-out-1.destination: ${error-topic}
spring.cloud.stream.kafka.binder.autoAlterTopics: true
spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.retention.ms: -1
spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.cleanup.policy: compact
spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.min.compaction.lag.ms: ${compaction-lag}
spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-1.producer.topic.properties.retention.ms: ${retention}
```

## Via ENV
- Kafka broker adress
- Opensearch uri

## Kubernetes
- unique label to identify all pipeline pods

## Create or Update a Processing Pipeline
POST /pipelines/{name}
```json
{
  "version": "app-version",
  "sourceTopic": "source-topic-name",
  "entityTopic": "entity-topic-name",
  "errRetentionHours": <error retention in hours>,
  "outCompactionLagHours": <compaction hold time in hours> 
  "pipeline": [
    {
      "name": "function-name",
      "function": "function",
      "parameters": {
        ...
      }
    },
    ...
  ]
}
```

## Delete a Processing Pipeline
DELETE /pipelines/{name}

## Get a Processing Pipeline Config
GET /pipelines/{name}

## Get all pipelines

GET /pipelines