package org.ct42.fnflow.manager.ui.view;

import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class LogView extends VerticalLayout {

    public LogView() {
        IFrame helpView = new IFrame();
        helpView.setName("helpview");
        helpView.setHeightFull();
        helpView.setWidthFull();
        add(helpView);
    }
}
