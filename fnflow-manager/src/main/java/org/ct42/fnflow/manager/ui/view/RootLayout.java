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

package org.ct42.fnflow.manager.ui.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ct42.fnflow.manager.pipeline.PipelineService;
import org.ct42.fnflow.manager.projector.ProjectorService;
import org.ct42.fnflow.manager.ui.Tree;
import com.vaadin.flow.component.applayout.DrawerToggle;

/**
 * @author Claas Thiele
 */
@RequiredArgsConstructor
@Layout
@Slf4j
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
public class RootLayout extends AppLayout implements AfterNavigationObserver {
    private final PipelineService pipelineService;
    private final ProjectorService projectorService;

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("FnFlow");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        addToNavbar(toggle, title);
        Tree tree = new Tree(pipelineService, projectorService);

        addToDrawer(tree);

        SplitLayout splitLayout = new SplitLayout(new EditorView(pipelineService), new LogView());
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.addThemeVariants(SplitLayoutVariant.LUMO_MINIMAL);
        splitLayout.setSplitterPosition(70);
        splitLayout.setSizeFull();
        splitLayout.addSplitterDragendListener(dragEndEvent -> {
            //trigger the resizing of the blockly editor
            UI.getCurrent().getPage().executeJs("window.dispatchEvent(new Event('resize'));");
        });
        setContent(splitLayout);
    }
}
