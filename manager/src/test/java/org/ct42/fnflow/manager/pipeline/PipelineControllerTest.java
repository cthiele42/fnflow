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

package org.ct42.fnflow.manager.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ct42.fnflow.manager.DeploymentDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Sajjad Safaeian
 */
@WebMvcTest
@ContextConfiguration(classes = {PipelineController.class})
public class PipelineControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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

    @Test
    void createPipelineTest() throws Exception {
        doNothing().when(pipelineService).createOrUpdate(any(), any());

        mockMvc.perform(
            post("/pipelines/{name}", "sample-name")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PIPELINE_CONFIG)
        )
        .andExpect(status().is2xxSuccessful());
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

        mockMvc.perform(get("/pipelines/{name}", "sample-name"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.version", is("0.0.9")))
        .andExpect(jsonPath("$.pipeline[0][0].name", is("idExist")));
    }

    @Test
    void getPipelineListTest() throws Exception {
        when(pipelineService.getList()).thenReturn(
            List.of(
                new DeploymentDTO("pipeline-name-1"),
                new DeploymentDTO("pipeline-name-2")
            )
        );

        mockMvc.perform(get("/pipelines"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.pipelines[0].name", is("pipeline-name-1")));
    }
}
