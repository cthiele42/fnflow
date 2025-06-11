package org.ct42.fnflow.manager.ui.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.manager.deployment.AbstractConfigDTO;
import org.ct42.fnflow.manager.deployment.DeploymentDoesNotExistException;
import org.ct42.fnflow.manager.deployment.DeploymentService;
import org.ct42.fnflow.manager.ui.Blockly;
import org.ct42.fnflow.manager.ui.DeploymentServiceUtil;
import org.ct42.fnflow.manager.ui.PrimeIcon;
import org.ct42.fnflow.manager.ui.Tree;

import java.util.Map;

import static org.ct42.fnflow.manager.ui.DeploymentServiceUtil.getDeploymentServiceInfoBasedOnKey;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Route("")
public class EditorView extends VerticalLayout {
    private static final String ID_PREFIX = "editor-";

    private final Map<String, DeploymentService<?>> deploymentServices;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Registration registration;
    private final TabSheet tabSheet = new TabSheet();

    public enum DeploymentEventAction {
        SAVE, GENERATE_JSON
    }

    @Getter
    public class CreateUpdateInitEvent extends ComponentEvent<EditorView> {
        private final String name;
        private final DeploymentEventAction action;

        public CreateUpdateInitEvent(String name, DeploymentEventAction action) {
            super(EditorView.this, false);
            this.name = name;
            this.action = action;
        }
    }

    public EditorView(Map<String, DeploymentService<?>> deploymentServices) {
        this.deploymentServices = deploymentServices;

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
                                DeploymentServiceUtil.DeploymentServiceInfo serviceInfo = getDeploymentServiceInfoBasedOnKey(event.getKey());
                                try {
                                    AbstractConfigDTO config = deploymentServices.get(serviceInfo.getServiceName()).getConfig(event.getName());
                                    openTab(event.getName(), event.getKey(), config);
                                } catch (DeploymentDoesNotExistException e) {
                                    Notification notification = Notification.show(String.format("Deployment of %s %s does not exist", serviceInfo.getType(), event.getName()));
                                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                    notification.setPosition(Notification.Position.BOTTOM_END);
                                    notification.setDuration(0);
                                }
                            }
                        }
                        case "new" -> {
                            CreateDialog dialog = new CreateDialog(getKeyPrefixBasedOnEvent(event));
                            dialog.setHeaderTitle("Name");
                            TextField nameField = new TextField();

                            nameField.addKeyPressListener(e -> {
                                if(e.getKey().equals(Key.ENTER)) {
                                    dialog.close();
                                    String name = nameField.getValue();
                                    String key = dialog.getKeyPrefix() + name;
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

    private void openTab(String name, String key, AbstractConfigDTO config) {
        try {
            ContextMenu contextMenu = new ContextMenu();
            contextMenu.addItem("Create/Update").addClickListener(event ->
                    ComponentUtil.fireEvent(UI.getCurrent(), new CreateUpdateInitEvent(name, DeploymentEventAction.SAVE))
            );

            contextMenu.addItem("Show JSON Config").addClickListener(event ->
                    ComponentUtil.fireEvent(UI.getCurrent(), new CreateUpdateInitEvent(name, DeploymentEventAction.GENERATE_JSON))
            );

            contextMenu.addItem("Close", event -> {
                Component c = contextMenu.getTarget();
                if(c instanceof Tab t) {
                    tabSheet.remove(tabSheet.getComponent(t));
                }
            });

            Icon icon = new Icon(VaadinIcon.ELLIPSIS_V);
            PrimeIcon typeIcon = new PrimeIcon(getDeploymentServiceInfoBasedOnKey(key).getIcon());
            Tab tab = new Tab(typeIcon, new Span(name), icon);
            tab.setId(ID_PREFIX + key);

            contextMenu.setTarget(tab);

            contextMenu.addOpenedChangeListener(e -> {
                if (e.isOpened()) {
                    tabSheet.setSelectedTab(tab);
                }
            });

            String configContent = config != null ? objectMapper.writeValueAsString(config) : null;
            Tab newTab = tabSheet.add(tab, new Blockly(configContent, key, deploymentServices));
            tabSheet.setSelectedTab(newTab);
        } catch (JsonProcessingException e) {
            Notification notification = Notification.show("Loading of deployment of processor " + name + " failed due to format errors");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.setPosition(Notification.Position.BOTTOM_END);
            notification.setDuration(0);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registration.remove();
    }

    private String getKeyPrefixBasedOnEvent(Tree.TreeActionEvent event) {
        return switch (event.getKey()) {
            case "procs" -> "proc-";
            case "projs" -> "projector-";
            default -> throw new IllegalStateException("Unexpected value: " + event.getKey());
        };
    }

    @Getter
    @RequiredArgsConstructor
    private static class CreateDialog extends Dialog {
        private final String keyPrefix;
    }

}
