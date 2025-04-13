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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        doNothing().when(pipelineService).createOrUpdatePipeline(any(), any());

        mockMvc.perform(
            post("/pipelines/{name}", "sample-name")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PIPELINE_CONFIG)
        )
        .andExpect(status().is2xxSuccessful());
    }
}
