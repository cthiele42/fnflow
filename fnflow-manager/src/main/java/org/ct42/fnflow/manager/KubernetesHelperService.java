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
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
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
    private static final String NAMESPACE = "default";
    private static final String APP_LABEL_KEY = "app.kubernetes.io/name";
    private static final String INSTANCE_LABEL_KEY = "app.kubernetes.io/instance";

    private static final String HTTP_SCHEMA = "HTTP";
    private static final String STARTUP_PATH = "/actuator/health/liveness";
    private static final String LIVENESS_PATH = "/actuator/health/liveness";
    private static final String READINESS_PATH = "/actuator/health/readiness";
    private static final Integer ACTUATOR_PORT = 8079;

    private final KubernetesClient k8sClient;
    private final ManagerProperties cfgProps;

    public void createOrUpdateDeployment(String appName, String deploymentName, String deploymentNamePrefix,
                                         String image, String imageVersion, List<String> args) {

        //TODO construct args for kafka batch and compression settings and group and destinations

        Map<String, String> selectorLabels = Map.of(
                APP_LABEL_KEY, appName,
                INSTANCE_LABEL_KEY, deploymentName
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
                                        .withName("actuator")
                                        .withContainerPort(ACTUATOR_PORT).build())
                                .withEnv(new EnvVarBuilder()
                                                .withName("OPENSEARCH_URIS")
                                                .withValue(cfgProps.getOsUris()).build(),
                                        new EnvVarBuilder()
                                                .withName("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS")
                                                .withValue(cfgProps.getKafkaBrokers()).build())
                                .withArgs(args)
                                .withStartupProbe(httpProbeCreator(STARTUP_PATH, ACTUATOR_PORT, HTTP_SCHEMA, 0, 1, 180))
                                .withLivenessProbe(httpProbeCreator(LIVENESS_PATH, ACTUATOR_PORT, HTTP_SCHEMA, 0, 10, 1))
                                .withReadinessProbe(httpProbeCreator(READINESS_PATH, ACTUATOR_PORT, HTTP_SCHEMA, 0, 1, 3))
                                .build())
                        .endSpec()
                        .endTemplate()
                        .endSpec().build())
                .forceConflicts().serverSideApply();
    }

    public DeploymentStatusDTO getDeploymentStatus(String deploymentName, String deploymentNamePrefix, String deploymentType)
            throws DeploymentDoesNotExistException {

        Deployment deployment = k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(deploymentNamePrefix + deploymentName).get();
        if(deployment == null)
            throw new DeploymentDoesNotExistException(deploymentName, deploymentType);
        return getDeploymentStatus(deployment);
    }

    public static DeploymentStatusDTO getDeploymentStatus(Deployment deployment) {
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

    public Container getDeploymentContainer(String deploymentName, String deploymentNamePrefix, String deploymentType)
            throws DeploymentDoesNotExistException {

        Deployment deployment = k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(deploymentNamePrefix + deploymentName).get();
        if(deployment == null)
            throw new DeploymentDoesNotExistException(deploymentName, deploymentType);

        return deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();
    }

    public List<DeploymentDTO> getDeploymentsByLabel(String appName, String deploymentNamePrefix) {
        return k8sClient.apps()
            .deployments().inNamespace(NAMESPACE).withLabel(APP_LABEL_KEY, appName)
            .list().getItems().stream()
            .map(deployment ->
                    new DeploymentDTO(
                        deployment.getMetadata().getName().replaceFirst(deploymentNamePrefix, "")
                    )
            ).toList();
    }

    /**
     * Creates an Informer for Deployments with <code>appName</code>.
     * Should be called only once by the using service.
     *
     * @param appName application label of the deployment which must match
     * @return an Informer for deployments with the given label
     */
    public SharedIndexInformer<Deployment> createDeploymentInformer(String appName) {
        return k8sClient.apps().deployments().inNamespace(NAMESPACE).withLabel(APP_LABEL_KEY, appName).inform(null, 10_000);
    }

    private Probe httpProbeCreator(String path, int port, String schema, int initialDelay, int period, int failure) {
        HTTPGetActionBuilder httpGetActionBuilder = new HTTPGetActionBuilder()
                .withPath(path)
                .withNewPort(port)
                .withScheme(schema);

        return new ProbeBuilder()
                .withHttpGet(httpGetActionBuilder.build())
                .withInitialDelaySeconds(initialDelay)
                .withPeriodSeconds(period)
                .withFailureThreshold(failure)
                .build();
    }
}
