package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;


public class PresentationPanel extends Composite {

  private static PresentationPanelUiBinder uiBinder = GWT.create(PresentationPanelUiBinder.class);

  interface PresentationPanelUiBinder extends UiBinder<Widget, PresentationPanel> {/* uibinder */}

  @UiField
  PresentationGroupPanel groupPanel;

  public PresentationPanel(String pUsername) {
    initWidget(uiBinder.createAndBindUi(this));
    groupPanel.setActiveUserName(pUsername);
  }

}
