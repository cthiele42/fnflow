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

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.function.Consumer;

/**
 * @author Claas Thiele
 */
@RequiredArgsConstructor
public class DeploymentInfoHandler implements ResourceEventHandler<Deployment> {
    private final Consumer<DeploymentInfo> listener;
    @Setter
    private String appPrefix;

    private DeploymentInfo mapToInfo(Deployment d) {
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setGeneration(d.getMetadata().getGeneration());
        deploymentInfo.setInternalName(d.getMetadata().getName());
        deploymentInfo.setName(d.getMetadata().getName().replaceFirst(appPrefix, ""));
        deploymentInfo.setCreationTimestamp(d.getMetadata().getCreationTimestamp());
        deploymentInfo.setStatus(KubernetesHelperService.getDeploymentStatus(d));
        return deploymentInfo;
    }

    @Override
    public void onAdd(Deployment deployment) {
        DeploymentInfo info = mapToInfo(deployment);
        info.setAction(DeploymentInfo.Action.ADD);
        listener.accept(info);
    }

    @Override
    public void onUpdate(Deployment oldDeployment, Deployment newDeployment) {
        DeploymentInfo info = mapToInfo(newDeployment);
        info.setAction(DeploymentInfo.Action.UPDATE);
        listener.accept(info);
    }

    @Override
    public void onDelete(Deployment deployment, boolean deletedFinalStateUnknown) {
        DeploymentInfo info = mapToInfo(deployment);
        info.setAction(DeploymentInfo.Action.DELETE);
        listener.accept(info);
    }
}
