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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.shared.Registration;
import org.ct42.fnflow.manager.pipeline.PipelineConfigDTO;
import org.ct42.fnflow.manager.pipeline.PipelineService;
import org.ct42.fnflow.manager.ui.view.EditorView;

/**
 * @author Claas Thiele
 */
@NpmPackage(value="@react-blockly/web", version="1.7.7")
@JsModule("Frontend/integration/react/blockly/blockly.tsx")
@Tag("fnflow-blockly")
public class Blockly extends ReactAdapterComponent {
    private static final String INITIAL_PROCESSOR_STATE = """
        {
          "version": "0.0.11",
          "sourceTopic": "input",
          "entityTopic": "output",
          "errorTopic": "error",
          "errRetentionHours": 336,
          "pipeline": [
        
          ]
        }""";

    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Registration registration;

    public void setWorkspaceState(String state) {
        setState("wsState", state);
    }

    public Blockly(String initialState, PipelineService pipelineService) {
        this.pipelineService = pipelineService;

        if (initialState != null) {
            setWorkspaceState(initialState);
        } else {
            setWorkspaceState(INITIAL_PROCESSOR_STATE);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registration = ComponentUtil.addListener(
                attachEvent.getUI(),
                EditorView.CreateUpdateInitEvent.class, // event class
                event -> {
                    String name = event.getName();
                    String wsCode = getState("wsCode", String.class);
                    try {
                        PipelineConfigDTO configDTO = objectMapper.readValue(wsCode, PipelineConfigDTO.class);
                        pipelineService.createOrUpdate(name, configDTO);
                    } catch (JsonProcessingException e) {
                        //TODO error handling
                    }
                });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registration.remove();
    }
}
