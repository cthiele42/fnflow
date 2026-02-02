package org.ct42.fnflow.manager.ui.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.shared.Registration;
import org.ct42.fnflow.manager.ui.Blockly;
import org.ct42.fnflow.manager.ui.PrimeIcon;

import static org.ct42.fnflow.manager.ui.DeploymentServiceUtil.getDeploymentServiceInfoBasedOnKey;

public class LogView extends VerticalLayout {

    private final TabSheet tabSheet = new TabSheet();
    private Registration registration;

    public LogView() {
        tabSheet.setWidth("100%");
        tabSheet.setMaxWidth("100%");
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_SMALL);

        add(tabSheet);

        this.setVisible(false);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registration = ComponentUtil.addListener(
            attachEvent.getUI(),
                Blockly.LogEvent.class,
                event -> {
                    Icon icon = new Icon(VaadinIcon.CLOSE);
                    icon.addClickListener(iconEvent -> {
                        iconEvent.getSource().getParent().ifPresent(component -> {
                            if(component instanceof Tab t) {
                                tabSheet.remove(tabSheet.getComponent(t));
                                if(0 == tabSheet.getTabCount()) {
                                    this.setVisible(false);
                                }
                            }
                        });
                    });

                    Tab newTab = switch (event.getType()) {
                        case "help" -> {
                            IFrame helpView = new IFrame();
                            helpView.setSrc(event.getContent());
                            helpView.setHeightFull();
                            helpView.setWidthFull();

                            PrimeIcon typeIcon = new PrimeIcon("pi pi-question-circle");

                            Tab tab = new Tab(typeIcon, new Span(event.getName() + " Help"), icon);
                            yield tabSheet.add(tab, helpView);
                        }
                        default -> {
                            Pre pre = new Pre();
                            String htmlContent =
                                    "<code style=\"font-family: monospace;\">" +
                                        event.getContent()
                                            .replace("&", "&amp;")
                                            .replace("<", "&lt;")
                                            .replace(">", "&gt;") +
                                    "</code>";

                            Html codeBlock = new Html(htmlContent);
                            pre.add(codeBlock);

                            PrimeIcon typeIcon = new PrimeIcon(getDeploymentServiceInfoBasedOnKey(event.getType()).getIcon());

                            Tab tab = new Tab(typeIcon, new Span(event.getName() + " JSON"), icon);
                            yield tabSheet.add(tab, pre);
                        }
                    };

                    tabSheet.setSelectedTab(newTab);
                    this.setVisible(true);
                }
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registration.remove();
    }

}