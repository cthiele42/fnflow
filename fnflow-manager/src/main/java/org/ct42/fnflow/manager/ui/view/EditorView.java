package org.ct42.fnflow.manager.ui.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.router.Route;

@Route("")
public class EditorView extends VerticalLayout {
    public EditorView() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidth("100%");
        tabSheet.setMaxWidth("100%");
        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_SMALL);
        add(tabSheet);
        tabSheet.add("...", new Div(new Text("This could be ...")));
    }
}
