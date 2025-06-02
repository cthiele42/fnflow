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

package org.ct42.fnflow.manager.deployment;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @param <DTO>
 *
 * @author Sajjad Safaeian
 */
@RequiredArgsConstructor
public abstract class AbstractDeploymentService<DTO extends AbstractConfigDTO> implements DeploymentService<DTO> {

    protected final KubernetesHelperService kubernetesHelperService;
    private SharedIndexInformer<Deployment> deploymentInformer;
    private final Map<Consumer<DeploymentInfo>, ResourceEventHandler<Deployment>> handlerLookup = new HashMap<>();

    @PostConstruct
    void initInformer() {
        deploymentInformer = kubernetesHelperService.createDeploymentInformer(getAppName());
    }

    @PreDestroy
    void cleanupInformer() {
        if(deploymentInformer != null) {
            deploymentInformer.close();
        }
    }

    @Override
    public void createOrUpdateAbstractConfig(String name, AbstractConfigDTO config) {
        createOrUpdate(name, (DTO)config);
    }

    @Override
    public DeploymentStatusDTO getStatus(String name) throws DeploymentDoesNotExistException {
        return kubernetesHelperService.getDeploymentStatus(name, getDeploymentNamePrefix(), getDeploymentType());
    }

    @Override
    public void delete(String name) throws DeploymentDoesNotExistException {
        getStatus(name);
        kubernetesHelperService.deleteDeployment(name, getDeploymentNamePrefix());
    }

    @Override
    public List<DeploymentDTO> getList() {
        return kubernetesHelperService.getDeploymentsByLabel(getAppName(), getDeploymentNamePrefix());
    }

    @Override
    public void addDeploymentInfoListener(Consumer<DeploymentInfo> listener) {
        if(!handlerLookup.containsKey(listener)) {
            DeploymentInfoHandler handler = new DeploymentInfoHandler(listener);
            handler.setAppPrefix(getDeploymentNamePrefix());
            handlerLookup.put(listener, handler);
            deploymentInformer.addEventHandler(handler);
        }
    }

    @Override
    public void removeDeploymentInfoListener(Consumer<DeploymentInfo> listener) {
        handlerLookup.computeIfPresent(listener, (l, h) -> {
            deploymentInformer.removeEventHandler(h);
            return null;
        });
    }
}
