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

package org.ct42.fnflow.manager.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * @author Sajjad Safaeian
 */
public class DeploymentServiceUtil {

    public static final List<DeploymentServiceInfo> DEPLOYMENT_SERVICE_INFOS =  List.of(
            new DeploymentServiceInfo("proc-", "processor", "pipelineService", "pi pi-cog"),
            new DeploymentServiceInfo("projector-", "projector", "projectorService", "pi pi-video")
    );

    public static DeploymentServiceInfo getDeploymentServiceInfoBasedOnKey(String key) {
        Optional<DeploymentServiceInfo> serviceInfoResult =
                DEPLOYMENT_SERVICE_INFOS.stream()
                        .filter(serviceInfo -> key.startsWith(serviceInfo.keyPrefix)).findFirst();
        return serviceInfoResult.orElse(DEPLOYMENT_SERVICE_INFOS.getFirst());
    }

    public static <T extends DeploymentServiceInfo> T getDeploymentServiceInfoBasedOnKey(String key, List<T> serviceInfos) {
        Optional<T> serviceInfoResult =
                serviceInfos.stream()
                        .filter(serviceInfo -> key.startsWith(serviceInfo.getKeyPrefix())).findFirst();
        return serviceInfoResult.orElse(serviceInfos.getFirst());
    }

    @Getter
    @RequiredArgsConstructor
    public static class DeploymentServiceInfo {
        private final String keyPrefix;
        private final String type;
        private final String serviceName;
        private final String icon;
    }
}
