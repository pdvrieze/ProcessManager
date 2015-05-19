package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;


public class WaitWidget extends Composite {

  private static WaitWidgetUiBinder uiBinder = GWT.create(WaitWidgetUiBinder.class);

  interface WaitWidgetUiBinder extends UiBinder<Widget, WaitWidget> {}

  public WaitWidget() {
    initWidget(uiBinder.createAndBindUi(this));
  }
}
