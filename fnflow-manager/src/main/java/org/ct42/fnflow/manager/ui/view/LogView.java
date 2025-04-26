package org.ct42.fnflow.manager.ui.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("logs")
public class LogView extends VerticalLayout {

    public LogView() {
        add(new H1("Here, log and help views will land soon!"));
    }
}
