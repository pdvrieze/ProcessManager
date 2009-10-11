package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class SplittedFillLeftPanel<T extends Widget> extends Composite {

  private T aTopLeftWidget;
  private Widget aBottomLeftWidget;
  private int aMinTopHeight = 150;
  public SplittedFillLeftPanel() {
    super();
    final SplittedPanel root = new SplittedPanel();
    initWidget(root);

    VerticalPanel leftPanel = new VerticalPanel();
    leftPanel.setWidth("100%");
    root.setLeftWidget(leftPanel);
    leftPanel.addStyleName("leftSplitPanel");
    root.addStyleName("pwt-SplittedFillLeftPanel");
  }

  @Override
  protected SplittedPanel getWidget() {
    return (SplittedPanel) super.getWidget();
  }

  private VerticalPanel getLeftPanel() {
    return (VerticalPanel) getWidget().getLeftWidget();
  }

  public void setTopLeftWidget(T pWidget) {
    VerticalPanel leftPanel = getLeftPanel();
    if (aTopLeftWidget!=null) {
      leftPanel.remove(aTopLeftWidget);
    }
    aTopLeftWidget = pWidget;
    leftPanel.insert(aTopLeftWidget, 0);
    leftPanel.setCellHeight(aTopLeftWidget, "100%");
    aTopLeftWidget.setWidth("100%");
    aTopLeftWidget.addStyleName("topLeftSplit");
  }

  public T getTopLeftWidget() {
    return aTopLeftWidget;
  }

  public void setBottomLeftWidget(Widget pWidget) {
    final VerticalPanel leftPanel = getLeftPanel();
    if (aBottomLeftWidget!=null) {
      leftPanel.remove(aBottomLeftWidget);
    }
    aBottomLeftWidget = pWidget;
    leftPanel.add(pWidget);
    pWidget.setWidth("100%");
    pWidget.addStyleName("bottomLeftSplit");
  }

  public Widget getBottomLeftWidget() {
    return aBottomLeftWidget;
  }

  public Widget getRightWidget() {
    return getWidget().getRightWidget();
  }

  public void setRightWidget(Widget pWidget) {
    getWidget().setRightWidget(pWidget);
    pWidget.addStyleDependentName("rightSplit");
  }

  public void setHeight(int pHeight) {
    int height = pHeight;
    height -= aBottomLeftWidget.getOffsetHeight();

    height -= 5;
    final int adjust = pHeight- height;
    final int listBoxHeight = Math.max(getMinTopHeight(), height);
    aTopLeftWidget.setHeight(listBoxHeight+"px");
    super.setHeight((listBoxHeight+adjust)+"px");

  }

  public void setMinTopHeight(int minTopHeight) {
    aMinTopHeight = minTopHeight;
  }

  public int getMinTopHeight() {
    return aMinTopHeight;
  }

}
