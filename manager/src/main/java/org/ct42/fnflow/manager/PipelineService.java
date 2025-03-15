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
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.manager.config.ManagerProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Claas Thiele
 */
@Service
@RequiredArgsConstructor
public class PipelineService {
    private static final String IMAGE="docker.io/ct42/fnflow-json-processors-kafka";
    private static final String NAME="fnflow-json-processors-kafka";
    private static final String NAMESPACE="default";

    private final KubernetesClient k8sClient;
    private final ManagerProperties cfgProps;

    public void createPipeline(String name, PipelineConfigDTO cfg) {
        List<String> args = new ArrayList<>();
        Arrays.stream(cfg.getPipeline()).forEach(f -> {
            String prefix = "--cfgfns." + f.getFunction() + "." + f.getName();
            f.getParameters().forEach((k, v) -> {
                if(v instanceof Map) { //for now, we support one nested map only
                    ((Map<?, ?>) v).forEach((k2, v2) -> args.add(prefix + "." + k + "." + k2 + "=" + v2));
                } else {
                    args.add(prefix + "." + k + "=" + v);
                }
            });
        });
        String definition = Arrays.stream(cfg.getPipeline())
                .map(PipelineConfigDTO.FunctionCfg::getName).collect(Collectors.joining("|"));
        args.add("--org.ct42.fnflow.function.definition=" + definition);

        //TODO construct args for kafka batch and compression settings and group and destinations


        Map<String, String> selectorLabels = Map.of(
                "app.kubernetes.io/name", NAME,
                "app.kubernetes.io/instance", name
        );

        k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .resource(new DeploymentBuilder().withNewMetadata()
                            .withName(NAME)
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
                .create();
    }
}
