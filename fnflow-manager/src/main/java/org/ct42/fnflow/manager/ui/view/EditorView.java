package org.ct42.fnflow.manager.ui.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class EditorView extends VerticalLayout {

    public EditorView() {
        add(new H1("Here, editors will land soon!"));
    }
}
