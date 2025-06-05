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

package org.ct42.fnflow.manager.deployment.projector;

import org.ct42.fnflow.manager.deployment.AbstractDeploymentController;
import org.ct42.fnflow.manager.deployment.DeploymentDTO;
import org.ct42.fnflow.manager.deployment.DeploymentStatusDTO;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Sajjad Safaeian
 */
@RestController
@RequestMapping(value = "/" + ProjectorController.APP_TYPE)
@RegisterReflectionForBinding(classes = {ProjectorConfigDTO.class, DeploymentStatusDTO.class, DeploymentDTO.class})
public class ProjectorController extends AbstractDeploymentController<ProjectorConfigDTO, ProjectorService> {
    public static final String APP_TYPE = "projectors";

    public ProjectorController(ProjectorService projectorService) {
        super(projectorService);
    }

    @Override
    protected String getApptype() {
        return APP_TYPE;
    }
}
