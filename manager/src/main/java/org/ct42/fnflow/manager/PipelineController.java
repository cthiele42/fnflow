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

import lombok.RequiredArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@RestController
@RequestMapping(value = "/pipelines")
@RequiredArgsConstructor
@RegisterReflectionForBinding(classes = {PipelineConfigDTO.class, PipelineConfigDTO.FunctionCfg.class, DeploymentStatusDTO.class})
public class PipelineController {
    private final PipelineService pipelineService;

    @PostMapping(value="/{name}")
    public ResponseEntity<Void> createPipeline(@PathVariable String name, @RequestBody PipelineConfigDTO cfg) {
        pipelineService.createOrUpdatePipeline(name, cfg);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value="/{name}/status")
    public DeploymentStatusDTO getPipelineStatus(@PathVariable String name) throws DeploymentDoesNotExistException {
        return pipelineService.getPipelineStatus(name);
    }

    @GetMapping(value="/{name}")
    public PipelineConfigDTO getPipelineConfig(@PathVariable String name) throws DeploymentDoesNotExistException {
        return pipelineService.getPipelineConfig(name);
    }

    @DeleteMapping(value="/{name}")
    public ResponseEntity<Void> deletePipeline(@PathVariable String name) {
        pipelineService.deletePipeline(name);
        return ResponseEntity.noContent().build();
    }
}
