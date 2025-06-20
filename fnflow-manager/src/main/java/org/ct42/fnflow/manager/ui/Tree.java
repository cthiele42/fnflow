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

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import elemental.json.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ct42.fnflow.manager.deployment.DeploymentDoesNotExistException;
import org.ct42.fnflow.manager.deployment.DeploymentInfo;
import org.ct42.fnflow.manager.deployment.DeploymentService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.ct42.fnflow.manager.ui.DeploymentServiceUtil.DEPLOYMENT_SERVICE_INFOS;
import static org.ct42.fnflow.manager.ui.DeploymentServiceUtil.getDeploymentServiceInfoBasedOnKey;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@NpmPackage(value="primereact", version="10.9.5")
@NpmPackage(value="primeicons", version="7.0.0")
@JsModule("Frontend/integration/react/tree/primereact-tree.tsx")
@Tag("pr-tree")
public class Tree extends ReactAdapterComponent {
    private final List<TreeDeploymentServiceInfo> TREE_DEPLOYMENT_SERVICE_INFOS =  List.of(
        new TreeDeploymentServiceInfo(DEPLOYMENT_SERVICE_INFOS.getFirst(), 0),
        new TreeDeploymentServiceInfo(DEPLOYMENT_SERVICE_INFOS.get(1), 1)
    );

    private final Map<String, DeploymentService<?>> deploymentServices;

    @Getter
    public class TreeActionEvent extends ComponentEvent<Tree> {
        private final String action;
        private final String key;
        private final String name;

        public TreeActionEvent(String action, String key, String name) {
            super(Tree.this, false);
            this.action = action;
            this.key = key;
            this.name = name;
        }
    }

    public Tree(Map<String, DeploymentService<?>> deploymentServices) {
        this.deploymentServices = deploymentServices;

        TreeNode processors = new TreeNode();
        processors.setLabel("Processors");
        processors.setKey("procs");
        processors.setSelectable(true);
        processors.setLeaf(true);
        processors.setIcon("pi pi-cog");

        TreeNode projectors = new TreeNode();
        projectors.setLabel("Projectors");
        projectors.setKey("projs");
        projectors.setSelectable(true);
        projectors.setLeaf(true);
        projectors.setIcon("pi pi-video");

        TreeNode topics = new TreeNode();
        topics.setLabel("Topics");
        topics.setKey("2");
        topics.setLeaf(true);
        topics.setSelectable(true);
        topics.setIcon("pi pi-bars");

        TreeNode[] roots = new TreeNode[]{processors, projectors, topics};

        setNodes(roots);
        setSelectedKey("procs");
    }

    public TreeNode[] getNodes() {
        return getState("nodes", TreeNode[].class);
    }

    public void setNodes(TreeNode[] nodes) {
        setState("nodes", nodes);
    }

    public String getSelectedKey() {
        return getState("selectedKey", String.class);
    }

    public void setSelectedKey(String selectedKey) {
        setState("selectedKey", selectedKey);
    }

    @RequiredArgsConstructor
    public class DeploymentChangeHandler implements Consumer<DeploymentInfo> {
        private final int rootNodeIndex;

        @Override
        public void accept(DeploymentInfo dI) {
            switch (dI.getAction()) {
                case ADD -> getUI().ifPresent(ui -> ui.access(() -> {
                    TreeNode[] nodes = Tree.this.getNodes();
                    TreeNode procs = nodes[rootNodeIndex];
                    if(procs.getChildren().stream().noneMatch(proc -> proc.getKey().equals(dI.getInternalName()))) {
                        TreeNode node = new TreeNode();
                        node.setKey(dI.getInternalName());
                        node.setLeaf(true);
                        node.setSelectable(true);
                        node.setLabel(dI.getName());
                        switch(dI.getStatus().getStatus()) {
                            case COMPLETED -> node.setIcon("pi pi-play-circle");
                            case PROGRESSING -> node.setIcon("pi pi-spinner-dotted pi-spin");
                            case FAILED -> node.setIcon("pi pi-times-circle");
                            case UNKNOWN -> node.setIcon("pi pi-question-circle");
                        }
                        long epochSeconds = Instant.parse(dI.getCreationTimestamp()).getEpochSecond();
                        node.setData(formatDurationWordsShort(
                                System.currentTimeMillis() - (epochSeconds * 1000L),
                                true,
                                true));
                        procs.getChildren().add(node);
                    }
                    procs.setLeaf(procs.getChildren().isEmpty());
                    Tree.this.setNodes(nodes);
                }));
                case DELETE -> getUI().ifPresent(ui -> ui.access(() -> {
                    TreeNode[] nodes = Tree.this.getNodes();
                    TreeNode procs = nodes[rootNodeIndex];
                    procs.getChildren().removeIf(n -> n.getKey().equals(dI.getInternalName()));
                    procs.setLeaf(procs.getChildren().isEmpty());
                    Tree.this.setNodes(nodes);
                }));
                case UPDATE -> getUI().ifPresent(ui -> ui.access(() -> {
                    TreeNode[] nodes = Tree.this.getNodes();
                    TreeNode procs = nodes[rootNodeIndex];
                    procs.getChildren().stream().filter(n -> n.getKey().equals(dI.getInternalName())).forEach(v -> {
                        v.setLabel(dI.getName());
                        long epochSeconds = Instant.parse(dI.getCreationTimestamp()).getEpochSecond();
                        v.setData(formatDurationWordsShort(
                                System.currentTimeMillis() - (epochSeconds * 1000L),
                                true,
                                true));
                        switch(dI.getStatus().getStatus()) {
                            case COMPLETED -> v.setIcon("pi pi-play-circle");
                            case PROGRESSING -> v.setIcon("pi pi-spinner-dotted pi-spin");
                            case FAILED -> v.setIcon("pi pi-times-circle");
                            case UNKNOWN -> v.setIcon("pi pi-question-circle");
                        }
                    });
                    Tree.this.setNodes(nodes);
                }));
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {

        TREE_DEPLOYMENT_SERVICE_INFOS.forEach(serviceInfo -> {
            if(serviceInfo.changeHandler == null) {
                serviceInfo.changeHandler = new DeploymentChangeHandler(serviceInfo.changeHandlerPosition);
                deploymentServices.get(serviceInfo.serviceInfo.getServiceName()).addDeploymentInfoListener(serviceInfo.changeHandler);
            }
        });

        getElement().addEventListener("execute", event -> {
            JsonObject cmd = event.getEventData().getObject("event.detail");
            String key = cmd.getString("key");
            String type = cmd.getString("type");

            DeploymentServiceUtil.DeploymentServiceInfo serviceInfo = getDeploymentServiceInfoBasedOnKey(key);
            String name = key.replaceFirst(serviceInfo.getKeyPrefix(), "");

            switch (type) {
                case "delete" -> {
                    try {
                        deploymentServices.get(serviceInfo.getServiceName()).delete(name);
                    } catch (DeploymentDoesNotExistException e) {
                        Notification notification = Notification.show(String.format("Deployment of %s %s does not exist", serviceInfo.getType(), name));
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        notification.setPosition(Notification.Position.BOTTOM_END);
                        notification.setDuration(0);
                    }
                }
                case "load" -> ComponentUtil.fireEvent(UI.getCurrent(), new TreeActionEvent("load", key, name));
                case "new" -> ComponentUtil.fireEvent(UI.getCurrent(), new TreeActionEvent("new", key, ""));
            }
        }).addEventData("event.detail");
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        TREE_DEPLOYMENT_SERVICE_INFOS.forEach(serviceInfo -> {
            if(serviceInfo.changeHandler != null) {
                deploymentServices.get(serviceInfo.serviceInfo.getServiceName()).removeDeploymentInfoListener(serviceInfo.changeHandler);
            }
        });
    }

    private String formatDurationWordsShort(
            final long durationMillis,
            final boolean suppressLeadingZeroElements,
            final boolean suppressTrailingZeroElements) {

        // This method is generally replaceable by the format method, but
        // there are a series of tweaks and special cases that require
        // trickery to replicate.
        String duration = DurationFormatUtils.formatDuration(durationMillis, "d'd 'H'h 'm'm 's's'");
        if (suppressLeadingZeroElements) {
            // this is a temporary marker on the front. Like ^ in regexp.
            duration = " " + duration;
            String tmp = StringUtils.replaceOnce(duration, " 0d", StringUtils.EMPTY);
            if (tmp.length() != duration.length()) {
                duration = tmp;
                tmp = StringUtils.replaceOnce(duration, " 0h", StringUtils.EMPTY);
                if (tmp.length() != duration.length()) {
                    duration = tmp;
                    tmp = StringUtils.replaceOnce(duration, " 0m", StringUtils.EMPTY);
                    duration = tmp;
                }
            }
            if (!duration.isEmpty()) {
                // strip the space off again
                duration = duration.substring(1);
            }
        }
        if (suppressTrailingZeroElements) {
            String tmp = StringUtils.replaceOnce(duration, " 0s", StringUtils.EMPTY);
            if (tmp.length() != duration.length()) {
                duration = tmp;
                tmp = StringUtils.replaceOnce(duration, " 0m", StringUtils.EMPTY);
                if (tmp.length() != duration.length()) {
                    duration = tmp;
                    tmp = StringUtils.replaceOnce(duration, " 0h", StringUtils.EMPTY);
                    if (tmp.length() != duration.length()) {
                        duration = StringUtils.replaceOnce(tmp, " 0d", StringUtils.EMPTY);
                    }
                }
            }
        }
        return duration.trim();
    }

    @RequiredArgsConstructor
    private static class TreeDeploymentServiceInfo {
        private final DeploymentServiceUtil.DeploymentServiceInfo serviceInfo;
        private final int changeHandlerPosition;
        private DeploymentChangeHandler changeHandler;
    }
}
