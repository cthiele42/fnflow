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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ct42.fnflow.manager.deployment.AbstractConfigDTO;
import org.ct42.fnflow.manager.deployment.DeploymentService;
import org.ct42.fnflow.manager.deployment.pipeline.PipelineConfigDTO;
import org.ct42.fnflow.manager.deployment.projector.ProjectorConfigDTO;
import org.ct42.fnflow.manager.ui.view.EditorView;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.ct42.fnflow.manager.ui.DeploymentServiceUtil.DEPLOYMENT_SERVICE_INFOS;
import static org.ct42.fnflow.manager.ui.DeploymentServiceUtil.getDeploymentServiceInfoBasedOnKey;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@NpmPackage(value="@react-blockly/web", version="1.7.7")
@JsModule("Frontend/integration/react/blockly/blockly.tsx")
@Tag("fnflow-blockly")
public class Blockly extends ReactAdapterComponent {
    private static final String INITIAL_PROCESSOR_STATE = """
        {
          "version": "0.0.13",
          "sourceTopic": "input",
          "entityTopic": "output",
          "cleanUpMode": "COMPACT",
          "cleanUpTimeHours": 336,
          "errorTopic": "error",
          "errRetentionHours": 336,
          "pipeline": [
        
          ]
        }""";

    private static final String INITIAL_PROJECTOR_STATE = """
        {
          "version": "0.0.3",
          "topic": "output",
          "index": "test-index"
        }""";

    private final List<BlocklyDeploymentServiceInfo> BLOCKLY_DEPLOYMENT_SERVICE_INFOS =  List.of(
            new BlocklyDeploymentServiceInfo(DEPLOYMENT_SERVICE_INFOS.getFirst(), INITIAL_PROCESSOR_STATE, PipelineConfigDTO.class),
            new BlocklyDeploymentServiceInfo(DEPLOYMENT_SERVICE_INFOS.get(1), INITIAL_PROJECTOR_STATE, ProjectorConfigDTO.class)
    );

    private final Map<String, DeploymentService<?>> deploymentServices;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String key;

    private Registration registration;

    public void setWorkspaceState(String state, String type) {
        setState("wsState", state);
        setState("wsType", type);
    }

    public Blockly(String initialState, String key, Map<String, DeploymentService<?>> deploymentServices) {
        this.deploymentServices = deploymentServices;
        this.key = key;

        BlocklyDeploymentServiceInfo serviceInfo = getDeploymentServiceInfoBasedOnKey(key, BLOCKLY_DEPLOYMENT_SERVICE_INFOS);

        String state = Objects.requireNonNullElse(initialState, serviceInfo.initialState);
        setWorkspaceState(state, serviceInfo.getType());
    }

    @ClientCallable
    public void showHelp(String type, String helpUrl) {
        ComponentUtil.fireEvent(UI.getCurrent(), new LogEvent(type, helpUrl));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registration = ComponentUtil.addListener(
                attachEvent.getUI(),
                EditorView.CreateUpdateInitEvent.class, // event class
                event -> {
                    String name = event.getName();
                    getElement().executeJs("return this.firstChild.firstChild.getAttribute('data-code')").then(String.class, code -> {
                        BlocklyDeploymentServiceInfo serviceInfo = getDeploymentServiceInfoBasedOnKey(key, BLOCKLY_DEPLOYMENT_SERVICE_INFOS);
                        try {
                            AbstractConfigDTO configDTO = objectMapper.readValue(code, serviceInfo.dtoClass);
                            deploymentServices.get(serviceInfo.getServiceName()).createOrUpdateAbstractConfig(name, configDTO);
                            Notification notification = Notification.show(
                                    String.format("%s %s successfully deployed", StringUtils.capitalize(serviceInfo.getType()), name)
                            );
                            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            notification.setPosition(Notification.Position.BOTTOM_END);
                            notification.setDuration(5000);

                        } catch (Exception e) {
                            Notification notification = Notification.show(String.format("Deployment of %s %s failed", serviceInfo.getType(), name));
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

    public static class BlocklyDeploymentServiceInfo extends DeploymentServiceUtil.DeploymentServiceInfo {
        private final String initialState;
        private final Class<? extends AbstractConfigDTO> dtoClass;

        public BlocklyDeploymentServiceInfo(DeploymentServiceUtil.DeploymentServiceInfo serviceInfo,
                                            String initialState, Class<? extends AbstractConfigDTO> dtoClass) {
            super(serviceInfo.getKeyPrefix(), serviceInfo.getType(), serviceInfo.getServiceName(), serviceInfo.getIcon());

            this.initialState = initialState;
            this.dtoClass = dtoClass;
        }
    }

    @Getter
    public class LogEvent extends ComponentEvent<Blockly> {
        private final String name;
        private final String content;

        public LogEvent(String name, String content) {
            super(Blockly.this, false);

            this.name = name;
            this.content = content;
        }
    }
}
