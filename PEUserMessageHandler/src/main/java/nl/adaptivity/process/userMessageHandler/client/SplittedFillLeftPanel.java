package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.user.client.ui.*;


public class SplittedFillLeftPanel<T extends Widget> extends ResizeComposite {

  private T mTopLeftWidget;

  private Widget mBottomLeftWidget;

  private int mMinTopHeight = 150;

  private final LayoutPanel mRightWidget;

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

    mRightWidget = new LayoutPanel();

    root.add(mRightWidget);
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

  public void setTopLeftWidget(final T widget) {
    final VerticalPanel leftPanel = getLeftPanel();
    if (mTopLeftWidget != null) {
      leftPanel.remove(mTopLeftWidget);
    }
    mTopLeftWidget = widget;
    leftPanel.insert(mTopLeftWidget, 0);
    leftPanel.setCellHeight(mTopLeftWidget, "100%");
    mTopLeftWidget.setWidth("100%");
    mTopLeftWidget.addStyleName("topLeftSplit");
  }

  public T getTopLeftWidget() {
    return mTopLeftWidget;
  }

  public void setBottomLeftWidget(final Widget widget) {
    final VerticalPanel leftPanel = getLeftPanel();
    if (mBottomLeftWidget != null) {
      leftPanel.remove(mBottomLeftWidget);
    }
    mBottomLeftWidget = widget;
    leftPanel.add(widget);
    widget.setWidth("100%");
    widget.addStyleName("bottomLeftSplit");
  }

  public Widget getBottomLeftWidget() {
    return mBottomLeftWidget;
  }

  public Widget getRightWidget() {
    if (mRightWidget.getWidgetCount() < 1) {
      return null;
    }
    return mRightWidget.getWidget(0);
  }

  public void setRightWidget(final Widget widget) {
    getWidget();
    if (mRightWidget.getWidgetCount() > 0) {
      mRightWidget.remove(0);
    }
    mRightWidget.add(widget);
    //    Layer layer = mRightWidget.getLayer(pWidget);
  }

  public void setHeight(final int height) {
    int actualHeight = height;
    actualHeight -= mBottomLeftWidget.getOffsetHeight();

    actualHeight -= 5;
    final int adjust = height - actualHeight;
    final int listBoxHeight = Math.max(getMinTopHeight(), actualHeight);
    mTopLeftWidget.setHeight(listBoxHeight + "px");
    super.setHeight((listBoxHeight + adjust) + "px");

  }

  public void setMinTopHeight(final int minTopHeight) {
    mMinTopHeight = minTopHeight;
  }

  public int getMinTopHeight() {
    return mMinTopHeight;
  }

}
