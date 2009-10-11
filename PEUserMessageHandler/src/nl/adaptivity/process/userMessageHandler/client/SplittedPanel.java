package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Widget;


public class SplittedPanel extends Composite {

  private HorizontalSplitPanel aMainPanel;

  public SplittedPanel() {
    aMainPanel = new HorizontalSplitPanel();

    int pos = (2*Window.getClientWidth())/9;
    pos = Math.max(pos, 100);
    aMainPanel.setSplitPosition(pos+"px");

    initWidget(aMainPanel);
  }

  protected void setLeftWidget(Widget pWidget) {
    aMainPanel.setLeftWidget(pWidget);
  }

  protected void setRightWidget(Widget pWidget) {
    aMainPanel.setRightWidget(pWidget);
  }

  public Widget getLeftWidget() {
    return aMainPanel.getLeftWidget();
  }

  public Widget getRightWidget() {
    return aMainPanel.getRightWidget();
  }
}
