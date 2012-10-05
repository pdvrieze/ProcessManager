package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.user.client.ui.*;


public class SplittedFillLeftPanel<T extends Widget> extends ResizeComposite {

  private T aTopLeftWidget;

  private Widget aBottomLeftWidget;

  private int aMinTopHeight = 150;

  private final LayoutPanel aRightWidget;

  public SplittedFillLeftPanel() {
    super();
    final SplitLayoutPanel root = new SplitLayoutPanel();
    initWidget(root);

    final VerticalPanel leftPanel = new VerticalPanel();
    leftPanel.setHeight("100%");
    leftPanel.setWidth("100%");
    root.addWest(leftPanel, 275d);
    leftPanel.addStyleName("leftSplitPanel");
    root.addStyleName("pwt-SplittedFillLeftPanel");

    aRightWidget = new LayoutPanel();

    root.add(aRightWidget);
    root.setWidgetMinSize(leftPanel, 200);
    //    root.layout();
  }

  @Override
  protected SplitLayoutPanel getWidget() {
    return (SplitLayoutPanel) super.getWidget();
  }

  private VerticalPanel getLeftPanel() {
    return (VerticalPanel) getWidget().getWidget(0);
  }

  public void setTopLeftWidget(final T pWidget) {
    final VerticalPanel leftPanel = getLeftPanel();
    if (aTopLeftWidget != null) {
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

  public void setBottomLeftWidget(final Widget pWidget) {
    final VerticalPanel leftPanel = getLeftPanel();
    if (aBottomLeftWidget != null) {
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
    if (aRightWidget.getWidgetCount() < 1) {
      return null;
    }
    return aRightWidget.getWidget(0);
  }

  public void setRightWidget(final Widget pWidget) {
    getWidget();
    if (aRightWidget.getWidgetCount() > 0) {
      aRightWidget.remove(0);
    }
    aRightWidget.add(pWidget);
    //    Layer layer = aRightWidget.getLayer(pWidget);
  }

  public void setHeight(final int pHeight) {
    int height = pHeight;
    height -= aBottomLeftWidget.getOffsetHeight();

    height -= 5;
    final int adjust = pHeight - height;
    final int listBoxHeight = Math.max(getMinTopHeight(), height);
    aTopLeftWidget.setHeight(listBoxHeight + "px");
    super.setHeight((listBoxHeight + adjust) + "px");

  }

  public void setMinTopHeight(final int minTopHeight) {
    aMinTopHeight = minTopHeight;
  }

  public int getMinTopHeight() {
    return aMinTopHeight;
  }

}
