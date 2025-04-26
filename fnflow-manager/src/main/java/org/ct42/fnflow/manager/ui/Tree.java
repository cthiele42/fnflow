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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ct42.fnflow.manager.DeploymentInfo;
import org.ct42.fnflow.manager.pipeline.PipelineService;
import org.ct42.fnflow.manager.projector.ProjectorService;

import java.util.function.Consumer;

/**
 * @author Claas Thiele
 */
@Slf4j
@NpmPackage(value="primereact", version="10.9.5")
@NpmPackage(value="primeicons", version="7.0.0")
@JsModule("Frontend/integration/react/primereact-tree.tsx")
@Tag("pr-tree")
public class Tree extends ReactAdapterComponent {
    private final PipelineService pipelineService;
    private DeploymentChangeHandler pipelineChangeHandler;

    private final ProjectorService projectorService;
    private DeploymentChangeHandler projectorChangeHandler;

    public Tree(PipelineService pipelineService, ProjectorService projectorService) {
        this.pipelineService = pipelineService;
        this.projectorService = projectorService;

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
            log.info("PROC: {}: {}", dI.getAction(), dI.getName());
            switch (dI.getAction()) {
                case ADD -> getUI().ifPresent(ui -> ui.access(() -> {
                    TreeNode[] nodes = Tree.this.getNodes();
                    TreeNode procs = nodes[rootNodeIndex];
                    if(procs.getChildren().stream().noneMatch(proc -> proc.getKey().equals(dI.getInternalName()))) {
                        TreeNode node = new TreeNode();
                        node.setKey(dI.getInternalName());
                        node.setLabel(dI.getName());
                        node.setLeaf(true);
                        node.setSelectable(true);
                        switch(dI.getStatus().getStatus()) {
                            case COMPLETED -> node.setIcon("pi pi-play-circle");
                            case PROGRESSING -> node.setIcon("pi pi-spinner-dotted pi-spin");
                            case FAILED -> node.setIcon("pi pi-times-circle");
                            case UNKNOWN -> node.setIcon("pi pi-question-circle");
                        }
                        procs.getChildren().add(node);
                    }
                    procs.setLeaf(procs.getChildren().isEmpty());
                    Tree.this.setNodes(nodes);
                }));
                case DELETE -> getUI().ifPresent(ui -> ui.access(() -> {
                    TreeNode[] nodes = Tree.this.getNodes();
                    TreeNode procs = nodes[rootNodeIndex];
                    procs.getChildren().removeIf(n -> n.getKey().equals(dI.getInternalName()));
                    Tree.this.setNodes(nodes);
                }));
                case UPDATE -> getUI().ifPresent(ui -> ui.access(() -> {
                    TreeNode[] nodes = Tree.this.getNodes();
                    TreeNode procs = nodes[rootNodeIndex];
                    procs.getChildren().stream().filter(n -> n.getKey().equals(dI.getInternalName())).forEach(v -> {
                        v.setLabel(dI.getName());
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
        if(pipelineChangeHandler == null) {
            pipelineChangeHandler = new DeploymentChangeHandler(0);
            pipelineService.addDeploymentInfoListener(pipelineChangeHandler);
        }
        if(projectorChangeHandler == null) {
            projectorChangeHandler = new DeploymentChangeHandler(1);
            projectorService.addDeploymentInfoListener(projectorChangeHandler);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(pipelineChangeHandler != null) {
            pipelineService.removeDeploymentInfoListener(pipelineChangeHandler);
        }
        if(projectorChangeHandler != null) {
            projectorService.removeDeploymentInfoListener(projectorChangeHandler);
        }
    }
}
