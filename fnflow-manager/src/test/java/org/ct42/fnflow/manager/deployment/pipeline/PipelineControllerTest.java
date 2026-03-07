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

package org.ct42.fnflow.manager.deployment.pipeline;

import org.ct42.fnflow.manager.deployment.DeploymentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author Sajjad Safaeian
 */
@SpringBootTest
@ContextConfiguration(classes = {PipelineController.class})
public class PipelineControllerTest {
    private RestTestClient client;

    @MockitoBean
    PipelineService pipelineService;

    private final static String PIPELINE_CONFIG =
    """
    {
        "version": "0.0.10",
        "sourceTopic": "input-topic",
        "entityTopic": "output-topic",
        "errorTopic": "error-topic",
        "errRetentionHours": 1,
        "pipeline": [
            [
                {
                    "name": "idExist",
                    "function": "hasValueValidator",
                    "parameters": {
                        "elementPath": "/id"
                    }
                },
                {
                    "name": "validateEmitter",
                    "function": "ChangeEventEmit",
                    "parameters": {
                        "eventContent": "",
                        "topic": "validate-topic"
                    }
                }
            ],
            {
                "name": "idMatch",
                "function": "Match",
                "parameters": {
                    "index": "testindex",
                    "template": "testtemplate",
                    "paramsFromInput": {
                        "ids": "/id"
                    },
                    "literalParams": {
                        "field": "id"
                    }
                }
            }
        ]
    }
    """;

    @BeforeEach
    public void initializeClient() {
     client = RestTestClient.bindToController(new PipelineController(pipelineService)).baseUrl("/pipelines").build();
    }

    @Test
    void createPipelineTest() {
        doNothing().when(pipelineService).createOrUpdate(any(), any());

        client.post()
            .uri("/{name}", "sample-name")
            .contentType(MediaType.APPLICATION_JSON)
            .body(PIPELINE_CONFIG)
            .exchange()
            .expectStatus().is2xxSuccessful();
    }

    @Test
    void getPipelineTest() throws Exception {
        PipelineConfigDTO dto = new PipelineConfigDTO();
        dto.setVersion("0.0.9");
        dto.setSourceTopic("sourceTopic");
        dto.setEntityTopic("entityTopic");
        dto.setErrorTopic("errorTopic");

        PipelineConfigDTO.FunctionCfg validCfg = new PipelineConfigDTO.FunctionCfg();
        validCfg.setFunction("hasValueValidator");
        validCfg.setName("idExist");
        validCfg.setParameters(Map.of("elementPath", "/id"));

        PipelineConfigDTO.FunctionCfg emitCfg = new PipelineConfigDTO.FunctionCfg();
        emitCfg.setFunction("ChangeEventEmit");
        emitCfg.setName("validateEmitter");
        emitCfg.setParameters(Map.of(
                "eventContent", "/",
                "topic", "validate-topic"
        ));
        PipelineConfigDTO.MultipleFunctions functions =
                new PipelineConfigDTO.MultipleFunctions(List.of(validCfg, emitCfg));


        PipelineConfigDTO.FunctionCfg matchCfg = new PipelineConfigDTO.FunctionCfg();
        matchCfg.setFunction("Match");
        matchCfg.setName("idMatch");
        matchCfg.setParameters(Map.of(
                "index", "testindex",
                "template", "testtemplate",
                "paramsFromInput", Map.of("ids", "/id"),
                "literalParams", Map.of("field", "id")));
        PipelineConfigDTO.SingleFunction match = new PipelineConfigDTO.SingleFunction(matchCfg);

        PipelineConfigDTO.FunctionCfg mergeCfg = new PipelineConfigDTO.FunctionCfg();
        mergeCfg.setFunction("MergeCreate");
        mergeCfg.setName("createmerge");
        mergeCfg.setParameters(Map.of(
                "mappings", List.of(
                        Map.of(
                                "from", "/id",
                                "to", "/identifier/id"
                        ),
                        Map.of(
                                "from", "/id",
                                "to", "/ID"
                        ),
                        Map.of(
                                "from", "/doesNotExist",
                                "to", "/foo"
                        )
                )
        ));
        PipelineConfigDTO.SingleFunction merge = new PipelineConfigDTO.SingleFunction(mergeCfg);

        dto.setPipeline(List.of(functions, match, merge));

        when(pipelineService.getConfig(any())).thenReturn(dto);

        client.get()
                .uri("/{name}", "sample-name")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                    .jsonPath("$.version").isEqualTo("0.0.9")
                    .jsonPath("$.pipeline[0][0].name").isEqualTo("idExist");
    }

    @Test
    void getPipelineListTest() {
        when(pipelineService.getList()).thenReturn(
            List.of(
                new DeploymentDTO("pipeline-name-1"),
                new DeploymentDTO("pipeline-name-2")
            )
        );

        client.get()
                .uri("")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                    .jsonPath("$.pipelines[0].name").isEqualTo("pipeline-name-1");
    }
}
