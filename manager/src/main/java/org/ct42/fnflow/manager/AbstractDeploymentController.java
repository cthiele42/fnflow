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

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 * @param <DTO>
 * @param <Service>
 *
 * @author Sajjad Safaiean
 */
@AllArgsConstructor
public abstract class AbstractDeploymentController<DTO, Service extends DeploymentService<DTO>> {

    private Service service;

    @PostMapping(value="/{name}")
    public ResponseEntity<Void> create(@PathVariable String name, @RequestBody DTO config) {
        service.createOrUpdate(name, config);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value="/{name}/status")
    public DeploymentStatusDTO getStatus(@PathVariable String name) throws DeploymentDoesNotExistException {
        return service.getStatus(name);
    }

    @GetMapping(value="/{name}")
    public DTO getConfig(@PathVariable String name) throws DeploymentDoesNotExistException {
        return service.getConfig(name);
    }

    @GetMapping
    public Map<String, List<DeploymentDTO>> getList(HttpServletRequest request) {
        String appType = request.getRequestURI().replace("/", "");

        return Map.of(appType, service.getList());
    }

    @DeleteMapping(value="/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        service.delete(name);
        return ResponseEntity.noContent().build();
    }

}
