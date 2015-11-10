package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Widget;


public class SplittedPanel extends Composite {

  private final HorizontalSplitPanel mMainPanel;

  public SplittedPanel() {
    mMainPanel = new HorizontalSplitPanel();

    int pos = (2 * Math.max(Window.getClientWidth(), 400)) / 9;
    pos = Math.max(pos, 100);
    mMainPanel.setSplitPosition(pos + "px");

    initWidget(mMainPanel);
  }

  protected void setLeftWidget(final Widget widget) {
    mMainPanel.setLeftWidget(widget);
  }

  protected void setRightWidget(final Widget widget) {
    mMainPanel.setRightWidget(widget);
  }

  public Widget getLeftWidget() {
    return mMainPanel.getLeftWidget();
  }

  public Widget getRightWidget() {
    return mMainPanel.getRightWidget();
  }
}
