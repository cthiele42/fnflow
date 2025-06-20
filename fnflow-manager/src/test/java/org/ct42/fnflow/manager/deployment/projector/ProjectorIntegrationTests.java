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

import org.ct42.fnflow.manager.AbstractIntegrationTests;
import org.ct42.fnflow.manager.deployment.DeploymentDTO;
import org.ct42.fnflow.manager.deployment.DeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Sajjad Safaeian
 */
@Testcontainers
@SpringBootTest(properties = {
        "deploymentcfg.os-uris=http://opensearch-cluster-master.default.svc:9200",
        "deploymentcfg.kafka-brokers=kafka.default.svc:9092"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ProjectorIntegrationTests extends AbstractIntegrationTests {

    @Autowired
    private ProjectorService projectorService;

    @Test
    void createOrUpdateTest() {
        //Given
        ProjectorConfigDTO config = getProjectorConfig();

        //When
        projectorService.createOrUpdate("projector-name", config);

        //Then
        thenCountOfPodRunningAndWithInstanceLabel("projector-name", 1);
        thenPodWithInstanceNameArgumentsContains("projector-name",
                "--fnflow.projector.index=entities-index",
                "--spring.cloud.stream.bindings.project-in-0.destination=entities-topic");
        thenDeploymentIsCompleted("projector-name");
    }

    @Test
    void deleteTest() throws Exception {
        //Given
        ProjectorConfigDTO config = getProjectorConfig();

        projectorService.createOrUpdate("projector-name", config);
        thenCountOfPodRunningAndWithInstanceLabel("projector-name", 1);

        //When
        projectorService.delete("projector-name");

        //Then
        thenCountOfPodRunningAndWithInstanceLabel("projector-name", 0);
    }

    @Test
    void getConfigTest() throws Exception {
        //Given
        ProjectorConfigDTO config = getProjectorConfig();

        projectorService.createOrUpdate("projector-name", config);
        thenCountOfPodRunningAndWithInstanceLabel("projector-name", 1);

        //When
        ProjectorConfigDTO result = projectorService.getConfig("projector-name");

        //Then
        then(result).isEqualTo(config);
    }

    @Test
    void getListTest() {
        //Given
        ProjectorConfigDTO config = getProjectorConfig();

        projectorService.createOrUpdate("projector-name-1", config);
        projectorService.createOrUpdate("projector-name-2", config);
        thenCountOfPodRunningAndWithInstanceLabel("projector-name-1", 1);
        thenCountOfPodRunningAndWithInstanceLabel("projector-name-2", 1);

        //When
        List<DeploymentDTO> deployments = projectorService.getList();

        //Then
        then(deployments)
            .hasSize(2)
            .extracting("name")
            .contains("projector-name-1", "projector-name-2");
    }

    @Override
    protected DeploymentService<?> getDeploymentService() {
        return projectorService;
    }

    private ProjectorConfigDTO getProjectorConfig() {
        ProjectorConfigDTO config = new ProjectorConfigDTO();
        config.setVersion("0.0.2");
        config.setTopic("entities-topic");
        config.setIndex("entities-index");

        return config;
    }
}
