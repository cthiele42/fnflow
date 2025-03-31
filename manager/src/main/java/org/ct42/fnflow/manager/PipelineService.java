/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.manager;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.manager.config.ManagerProperties;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Service
@RequiredArgsConstructor
public class PipelineService {
    private static final String IMAGE="docker.io/ct42/fnflow-json-processors-kafka";
    private static final String NAME="fnflow-json-processors-kafka";
    private static final String NAMESPACE="default";
    private static final String PROCESSOR_PREFIX="proc-";

    private final KubernetesClient k8sClient;
    private final ManagerProperties cfgProps;

    public void createOrUpdatePipeline(String name, PipelineConfigDTO cfg) {
        List<String> args = new ArrayList<>();
        Arrays.stream(cfg.getPipeline()).forEach(f -> {
            String prefix = "--cfgfns." + f.getFunction() + "." + f.getName();
            f.getParameters().forEach((k, v) -> {
                if(v instanceof Map) { //for now, we support one nested map only
                    ((Map<?, ?>) v).forEach((k2, v2) -> args.add(prefix + "." + k + "." + k2 + "=" + v2));
                } if(v instanceof List) {
                    for(int i=0; i<((List<?>) v).size(); i++) {
                        String elemPrefix = prefix + "." + k + "." + i;
                        Object elem = ((List<?>) v).get(i);
                        if(elem instanceof Map) { // we support Map here only (MapCreate mappings config)
                            ((Map<?, ?>) elem).forEach((k2, v2) -> args.add(elemPrefix + "." + k2 + "=" + v2));
                        } else {
                            args.add(elemPrefix + "=" + elem);
                        }
                    }
                } else {
                    args.add(prefix + "." + k + "=" + v);
                }
            });
        });
        String definition = Arrays.stream(cfg.getPipeline())
                .map(PipelineConfigDTO.FunctionCfg::getName).collect(Collectors.joining("|"));
        args.add("--org.ct42.fnflow.function.definition=" + definition);
        args.add("--spring.cloud.stream.kafka.default.producer.compression-type=lz4");
        args.add("--spring.cloud.stream.kafka.default.producer.configuration.batch.size=131072");
        args.add("--spring.cloud.stream.kafka.default.producer.configuration.linger.ms=50");
        args.add("--spring.cloud.stream.default.group=" + name);
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=" + cfg.getSourceTopic());
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-0.destination=" + cfg.getEntityTopic());
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-1.destination=" + cfg.getErrorTopic());
        args.add("--spring.cloud.stream.kafka.binder.autoAlterTopics=true");
        args.add("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-1.producer.topic.properties.retention.ms="
                + convertHoursToMilliseconds(cfg.getErrRetentionHours()));



        //TODO construct args for kafka batch and compression settings and group and destinations


        Map<String, String> selectorLabels = Map.of(
                "app.kubernetes.io/name", NAME,
                "app.kubernetes.io/instance", name
        );

        k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .resource(new DeploymentBuilder().withNewMetadata()
                            .withName(PROCESSOR_PREFIX + name)
                            .withNamespace(NAMESPACE)
                            .withLabels(selectorLabels)
                        .endMetadata()
                        .withNewSpec()
                        .withReplicas(1)
                        .withNewSelector()
                            .withMatchLabels(selectorLabels)
                        .endSelector()
                        .withNewTemplate()
                            .withNewMetadata()
                                .withLabels(selectorLabels)
                            .endMetadata()
                            .withNewSpec()
                                .withContainers(new ContainerBuilder().withName(name)
                                    .withImage(IMAGE + ":" + cfg.getVersion())
                                    .withImagePullPolicy("IfNotPresent")
                                    .withPorts(new ContainerPortBuilder()
                                            .withName("http")
                                            .withContainerPort(8080).build())
                                    .withEnv(new EnvVarBuilder()
                                                .withName("OPENSEARCH_URIS")
                                                .withValue(cfgProps.getOsUris()).build(),
                                            new EnvVarBuilder()
                                                .withName("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS")
                                                .withValue(cfgProps.getKafkaBrokers()).build())
                                    .withArgs(args).build())
                            .endSpec()
                        .endTemplate()
                        .endSpec().build())
                .forceConflicts().serverSideApply();
    }

    public DeploymentStatusDTO getPipelineStatus(String name) {
        Deployment deployment = k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(PROCESSOR_PREFIX + name).get();
        DeploymentStatusDTO status = new DeploymentStatusDTO();
        DeploymentStatus deploymentStatus = deployment.getStatus();
        Integer specReplicas = deployment.getSpec().getReplicas();

        if(deploymentStatus.getObservedGeneration() != null) {
            Integer updatedReplicas = deploymentStatus.getUpdatedReplicas();
            Integer availableReplicas = deploymentStatus.getAvailableReplicas();
            Integer replicas = deploymentStatus.getReplicas();

            Optional<DeploymentCondition> progressing = deploymentStatus.getConditions().stream().filter(c -> c.getType().equals("Progressing")).findFirst();
            if(progressing.isPresent()) {
                if(progressing.get().getReason().equals("ProgressDeadlineExceeded")) {
                    status.setStatus(DeploymentStatusDTO.Status.FAILED);
                    status.setMessage(progressing.get().getMessage());
                    return status;
                }
            }
            if(specReplicas != null && Optional.ofNullable(updatedReplicas).orElse(0) < specReplicas) {
                status.setStatus(DeploymentStatusDTO.Status.PROGRESSING);
                status.setMessage(String.format("Waiting for rollout to finish: %d out of %d new replicas have been updated...", updatedReplicas, specReplicas));
                return status;
            }
            if(Optional.ofNullable(replicas).orElse(0) > Optional.ofNullable(updatedReplicas).orElse(0)) {
                status.setStatus(DeploymentStatusDTO.Status.PROGRESSING);
                status.setMessage(String.format("Waiting for rollout to finish: %d old replicas are pending termination...", replicas - updatedReplicas));
                return status;
            }
            if(availableReplicas != null && availableReplicas < Optional.ofNullable(updatedReplicas).orElse(0)) {
                status.setStatus(DeploymentStatusDTO.Status.PROGRESSING);
                status.setMessage(String.format("Waiting for rollout to finish: %d of %d updated replicas are available...", availableReplicas, updatedReplicas));
                return status;
            }
            status.setStatus(DeploymentStatusDTO.Status.COMPLETED);
            status.setMessage("Successfully rolled out");
            return status;
        }
        return status;
    }

    public void deletePipeline(String name) {
        k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(PROCESSOR_PREFIX + name).delete();
    }

    private Long convertHoursToMilliseconds(int hours) {
        return hours * 60L * 60L * 1000L;
    }
}
