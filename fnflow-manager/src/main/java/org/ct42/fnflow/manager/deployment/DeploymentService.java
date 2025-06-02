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

import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @param <DTO>
 *
 * @author Sajjad Safaeian
 */
public interface DeploymentService<DTO extends AbstractConfigDTO> {

    void createOrUpdate(String name, DTO config);

    void createOrUpdateAbstractConfig(String name, AbstractConfigDTO config);

    DeploymentStatusDTO getStatus(String name) throws DeploymentDoesNotExistException;

    void delete(String name) throws DeploymentDoesNotExistException;

    DTO getConfig(String name) throws DeploymentDoesNotExistException;

    List<DeploymentDTO> getList();

    String getAppName();

    String getDeploymentNamePrefix();

    String getDeploymentType();

    void addDeploymentInfoListener(Consumer<DeploymentInfo> listener);

    void removeDeploymentInfoListener(Consumer<DeploymentInfo> listener);
}
