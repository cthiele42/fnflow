package org.ct42.fnflow.manager.ui.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import org.ct42.fnflow.manager.DeploymentDoesNotExistException;
import org.ct42.fnflow.manager.pipeline.PipelineConfigDTO;
import org.ct42.fnflow.manager.pipeline.PipelineService;
import org.ct42.fnflow.manager.ui.Blockly;
import org.ct42.fnflow.manager.ui.Tree;

@Route("")
public class EditorView extends VerticalLayout {
    private static final String ID_PREFIX = "editor-";

    private final PipelineService pipelineService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Registration registration;
    private final TabSheet tabSheet = new TabSheet();

    @Getter
    public class CreateUpdateInitEvent extends ComponentEvent<EditorView> {
        private final String name;

        public CreateUpdateInitEvent(String name) {
            super(EditorView.this, false);
            this.name = name;
        }
    }

    public EditorView(PipelineService pipelineService) {
        this.pipelineService = pipelineService;

        tabSheet.setWidth("100%");
        tabSheet.setMaxWidth("100%");
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_SMALL);
        tabSheet.addSelectedChangeListener(event -> {
            //trigger resizing and repainting of blockly editors as editors in hidden tabs were resized to 0
            UI.getCurrent().getPage().executeJs("window.dispatchEvent(new Event('resize'));");
        });
        add(tabSheet);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registration = ComponentUtil.addListener(
                attachEvent.getUI(),
                Tree.TreeActionEvent.class,
                event -> {
                    switch (event.getAction()) {
                        case "load" -> {
                            boolean tabExists = false;
                            for(int t = 0; t < tabSheet.getTabCount(); t++) {
                                if(tabSheet.getTabAt(t).getId().orElse("").equals(ID_PREFIX + event.getKey())) { // tab already exist
                                    tabExists = true;
                                    tabSheet.setSelectedIndex(t);
                                    break;
                                }
                            }
                            if(!tabExists) {
                                try {
                                    PipelineConfigDTO config = pipelineService.getConfig(event.getName());
                                    openTab(event.getName(), event.getKey(), config);
                                } catch (DeploymentDoesNotExistException e) {
                                    //TODO handle error
                                }
                            }
                        }
                        case "new" -> {
                            Dialog dialog = new Dialog();
                            dialog.setHeaderTitle("Name");
                            TextField nameField = new TextField();
                            nameField.addKeyPressListener(e -> {
                                if(e.getKey().equals(Key.ENTER)) {
                                    dialog.close();
                                    String name = nameField.getValue();
                                    String key = "proc-" + name;
                                    openTab(name, key, null);
                                } else if(e.getKey().equals(Key.ESCAPE)) {
                                    dialog.close();
                                }
                            });
                            nameField.focus();
                            dialog.add(nameField);
                            dialog.open();
                        }
                    }
                }
        );
    }

    private void openTab(String name, String key, PipelineConfigDTO config) {
        try {
            ContextMenu contextMenu = new ContextMenu();
            contextMenu.setOpenOnClick(true);
            contextMenu.addItem("Create/Update").addClickListener(event ->
                    ComponentUtil.fireEvent(UI.getCurrent(), new CreateUpdateInitEvent(name)));
            contextMenu.addItem("Close", event ->
                    contextMenu.getTarget().getParent().ifPresent(c -> {
                        if(c instanceof Tab t) {
                            tabSheet.remove(tabSheet.getComponent(t));
                        }
                    }));

            Icon icon = new Icon(VaadinIcon.ELLIPSIS_V);
            contextMenu.setTarget(icon);

            Tab tab = new Tab(new Span(name), icon);
            tab.setId(ID_PREFIX + key);

            Tab newTab = tabSheet.add(tab, new Blockly(config != null ? objectMapper.writeValueAsString(config) : null, pipelineService));
            tabSheet.setSelectedTab(newTab);
        } catch (JsonProcessingException e) {
            //TODO handle error
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registration.remove();
    }
}
