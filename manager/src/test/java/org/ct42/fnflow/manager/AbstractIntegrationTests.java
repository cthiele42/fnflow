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

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

/**
 * @author Sajjad Safaeian
 */
public abstract class AbstractIntegrationTests {

    protected abstract DeploymentService<?> getDeploymentService();

    @Container
    static final K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"));

    @TestBean
    KubernetesClient kubernetesClient;

    static KubernetesClient kubernetesClient() {
        String config = k3s.getKubeConfigYaml();
        KubernetesClientBuilder builder = new KubernetesClientBuilder();

        // we do this in a conditional manner to satisfy processTestAOT
        if(config != null) {
            Config kubeconfig = Config.fromKubeconfig(config);
            builder.withConfig(kubeconfig);
        }
        return builder.build();
    }

    protected void thenCountOfPodRunningAndWithInstanceLabel(String instanceName, int count) {
        await().atMost(Duration.ofSeconds(180)).untilAsserted(() -> assertThat(kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("app.kubernetes.io/instance", instanceName)
                .list().getItems().stream().filter(p -> p.getStatus().getPhase().equals("Running")).collect(Collectors.toList())).hasSize(count));
    }

    protected void thenDeploymentIsCompleted(String pipelineName) {
        await()
            .atMost(Duration.ofSeconds(180))
            .untilAsserted(() ->
                assertThat(
                    getDeploymentService().getStatus(pipelineName).getStatus()
                ).isEqualTo(DeploymentStatusDTO.Status.COMPLETED)
            );
    }

    protected void thenPodWithInstanceNameArgumentsContains(String name, String... args) {
        then(kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("app.kubernetes.io/instance", name)
                .list().getItems().getFirst().getSpec().getContainers().getFirst().getArgs()).contains(args);
    }

}
