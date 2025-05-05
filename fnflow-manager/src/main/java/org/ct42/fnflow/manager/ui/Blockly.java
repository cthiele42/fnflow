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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
          "cleanUpMode": "COMPACT",
          "cleanUpTimeHours": 336,
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
                    getElement().executeJs("return this.firstChild.getAttribute('data-code')").then(String.class, code -> {
                        try {
                            PipelineConfigDTO configDTO = objectMapper.readValue(code, PipelineConfigDTO.class);
                            pipelineService.createOrUpdate(name, configDTO);
                            Notification notification = Notification.show("Processor " + name + " successfully deployed");
                            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            notification.setPosition(Notification.Position.BOTTOM_END);
                            notification.setDuration(5000);

                        } catch (JsonProcessingException e) {
                            Notification notification = Notification.show("Deployment of processor " + name + " failed");
                            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                            notification.setPosition(Notification.Position.BOTTOM_END);
                            notification.setDuration(0);
                        }
                    });
                });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registration.remove();
    }
}
