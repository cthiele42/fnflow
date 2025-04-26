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

package org.ct42.fnflow.manager.projector;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.manager.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Sajjad Safaeian
 */
@Service
@RequiredArgsConstructor
public class ProjectorService implements DeploymentService<ProjectorConfigDTO> {

    private static final String IMAGE = "docker.io/ct42/fnflow-projector";
    private static final String APP_NAME = "fnflow-projector";
    private static final String PROJECTOR_PREFIX = "projector-";

    private final KubernetesHelperService kubernetesHelperService;
    private SharedIndexInformer<Deployment> deploymentInformer;
    private Map<Consumer<DeploymentInfo>, ResourceEventHandler<Deployment>> handlerLookup = new HashMap<>();

    @Override
    public void createOrUpdate(String name, ProjectorConfigDTO config) {
        List<String> args = new ArrayList<>();

        args.add("--fnflow.projector.index=" + config.getIndex());
        args.add("--spring.cloud.stream.bindings.project-in-0.destination=" + config.getTopic());
        args.add("--spring.cloud.stream.default.group=" + name);

        kubernetesHelperService.createOrUpdateDeployment(APP_NAME, name, PROJECTOR_PREFIX, IMAGE, config.getVersion(), args);
    }

    @Override
    public DeploymentStatusDTO getStatus(String name) throws DeploymentDoesNotExistException {
        return kubernetesHelperService.getDeploymentStatus(name, PROJECTOR_PREFIX);
    }

    @Override
    public void delete(String name) {
        kubernetesHelperService.deleteDeployment(name, PROJECTOR_PREFIX);
    }

    @Override
    public ProjectorConfigDTO getConfig(String name) throws DeploymentDoesNotExistException {
        Container container = kubernetesHelperService.getDeploymentContainer(name, PROJECTOR_PREFIX);

        ProjectorConfigDTO config = new ProjectorConfigDTO();

        String image = container.getImage();
        config.setVersion(image.substring(image.lastIndexOf(":") + 1));

        container.getArgs().forEach(arg -> {
            if(arg.startsWith("--fnflow.projector.index=")) {
                config.setIndex(arg.substring(arg.lastIndexOf("=") + 1));
            } else if(arg.startsWith("--spring.cloud.stream.bindings.project-in-0.destination=")) {
                config.setTopic(arg.substring(arg.lastIndexOf("=") + 1));
            }
        });

        return config;
    }

    @Override
    public List<DeploymentDTO> getList() {
        return kubernetesHelperService.getDeploymentsByLabel(APP_NAME, PROJECTOR_PREFIX);
    }

    @PostConstruct
    void initInformer() {
        deploymentInformer = kubernetesHelperService.createDeploymentInformer(APP_NAME);
    }

    @PreDestroy
    void cleanupInformer() {
        if(deploymentInformer != null) {
            deploymentInformer.close();
        }
    }

    public void addDeploymentInfoListener(Consumer<DeploymentInfo> listener) {
        if(!handlerLookup.containsKey(listener)) {
            DeploymentInfoHandler handler = new DeploymentInfoHandler(listener);
            handler.setAppPrefix(PROJECTOR_PREFIX);
            handlerLookup.put(listener, handler);
            deploymentInformer.addEventHandler(handler);
        }
    }

    public void removeDeploymentInfoListener(Consumer<DeploymentInfo> listener) {
        handlerLookup.computeIfPresent(listener, (l, h) -> {
            deploymentInformer.removeEventHandler(h);
            return null;
        });
    }
}
