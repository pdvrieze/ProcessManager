package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;


public class AboutPanel extends Widget {

  private static AboutPanelUiBinder uiBinder = GWT.create(AboutPanelUiBinder.class);

  interface AboutPanelUiBinder extends UiBinder<Element, AboutPanel> {}

  public AboutPanel() {
    setElement(uiBinder.createAndBindUi(this));
  }

}
