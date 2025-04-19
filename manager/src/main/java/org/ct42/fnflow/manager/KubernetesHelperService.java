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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.manager.config.ManagerProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Sajjad Safaeoan
 */
@Service
@RequiredArgsConstructor
public class KubernetesHelperService {
    private static final String NAMESPACE="default";

    private final KubernetesClient k8sClient;
    private final ManagerProperties cfgProps;

    public void createOrUpdateDeployment(String appName, String deploymentName, String deploymentNamePrefix,
                                         String image, String imageVersion, List<String> args) {

        //TODO construct args for kafka batch and compression settings and group and destinations

        Map<String, String> selectorLabels = Map.of(
                "app.kubernetes.io/name", appName,
                "app.kubernetes.io/instance", deploymentName
        );

        k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .resource(new DeploymentBuilder().withNewMetadata()
                        .withName(deploymentNamePrefix + deploymentName)
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
                        .withContainers(new ContainerBuilder().withName(deploymentName)
                                .withImage(image + ":" + imageVersion)
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

    public DeploymentStatusDTO getDeploymentStatus(String deploymentName, String deploymentNamePrefix)
            throws DeploymentDoesNotExistException {

        Deployment deployment = k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(deploymentNamePrefix + deploymentName).get();
        if(deployment == null) throw new DeploymentDoesNotExistException(deploymentName);

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

    public void deleteDeployment(String deploymentName, String deploymentNamePrefix) {
        k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(deploymentNamePrefix + deploymentName).delete();
    }

    public Container getDeploymentContainer(String deploymentName, String deploymentNamePrefix)
            throws DeploymentDoesNotExistException {

        Deployment deployment = k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(deploymentNamePrefix + deploymentName).get();
        if(deployment == null) throw new DeploymentDoesNotExistException(deploymentName);

        return deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();
    }
}
