package org.ct42.fnflow.manager.ui.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.shared.Registration;
import org.ct42.fnflow.manager.ui.Blockly;

public class LogView extends VerticalLayout {

    private final TabSheet tabSheet = new TabSheet();
    private Registration registration;

    public LogView() {
        tabSheet.setWidth("100%");
        tabSheet.setMaxWidth("100%");
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_SMALL);
        tabSheet.addSelectedChangeListener(event -> {
            //trigger resizing and repainting of blockly editors as editors in hidden tabs were resized to 0
            UI.getCurrent().getPage().executeJs("window.dispatchEvent(new Event('resize'));");
        });
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
                    IFrame helpView = new IFrame();
                    helpView.setSrc(event.getContent());
                    helpView.setHeightFull();
                    helpView.setWidthFull();

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
                    Tab tab = new Tab(new Span(event.getName() + " Help"), icon);

                    Tab newTab = tabSheet.add(tab, helpView);
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