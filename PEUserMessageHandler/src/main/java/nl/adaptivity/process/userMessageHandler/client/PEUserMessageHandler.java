package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PEUserMessageHandler implements EntryPoint, ValueChangeHandler<String>, SelectionHandler<Integer>, ResizeHandler {

  interface MyUiBinder extends UiBinder<Widget, PEUserMessageHandler> {

    MyUiBinder INSTANCE = GWT.create(MyUiBinder.class);
  }

  static final String BASEURL = ""/* "http://localhost:8192/ProcessEngine/" */;

  private static final int REFRESH_INTERVAL = 2000;

  private static final Boolean DEFAULT_REFRESH = false;

  private Label mStatusLabel;

  private CheckBox mRefreshCheckbox;

  private TabLayoutPanel mTabPanel;

  @SuppressWarnings("unused")
  private HandlerRegistration mHistoryHandler;

  private ProcessesPanel mProcessesPanel;

  private InstancesPanel mInstancesPanel;

  private TasksPanel mTasksPanel;

  private RootLayoutPanel mRootPanel;

  private DockLayoutPanel mDockPanel;

  private FlowPanel mStatusPanel;

  @UiField
  InlineLabel nStatusLabel;

  @UiField
  CheckBox nRefreshCheckbox;

  /**
   * This is the entry point method.
   *
   * @category UI
   */
  @Override
  public void onModuleLoad() {
    final String initToken = History.getToken();
    if (initToken.length() == 0) {
      History.newItem("Processes");
    }

    final Widget w = MyUiBinder.INSTANCE.createAndBindUi(this);
    mRootPanel = RootLayoutPanel.get();
    mRootPanel.add(w);

    mDockPanel = new DockLayoutPanel(Unit.PX);
    mDockPanel.addNorth(new HTML("<h1 class=\"title\">Process Engine Interface</h1>"), 25);
    //    mDockPanel.addStyleName("dockPanel");
    mRootPanel.add(mDockPanel);

    mTabPanel = new TabLayoutPanel(22, Unit.PX);
    mTabPanel.addStyleName("tabPanel");
    //    mDockPanel.setCellHeight(mTabPanel, "100%");

    mStatusLabel = new Label();
    mStatusLabel.setText("Initializing...");
    mStatusLabel.addStyleName("statusPanel-left");


    mProcessesPanel = createProcessesPanel();

    mInstancesPanel = createInstancesPanel();

    mTasksPanel = createTaskPanel();
    mTabPanel.add(mProcessesPanel, "Processes");
    mTabPanel.add(mInstancesPanel, "Instances");
    mTabPanel.add(mTasksPanel, "Tasks");
    mTabPanel.selectTab(0);

    mTabPanel.addSelectionHandler(this);

    mStatusPanel = new FlowPanel();
    mStatusPanel.add(mStatusLabel);
    mStatusPanel.addStyleName("statusPanel");

    mRefreshCheckbox = new CheckBox("refresh");
    mRefreshCheckbox.setValue(DEFAULT_REFRESH);
    mRefreshCheckbox.addStyleName("statusPanel-right");
    mStatusPanel.add(mRefreshCheckbox);

    if (!GWT.isScript()) {
      final Label label = new Label("Hosted mode");
      label.addStyleName("span");
      mStatusPanel.add(label);
    } else {
      mStatusPanel.add(new HTML("Compiled mode"));
    }

    //    mDockPanel.addSouth(new HTML("South"), 20);
    mDockPanel.addSouth(mStatusPanel, 20d);
    //    mDockPanel.setCellHeight(mTabPanel, "100%");

    mDockPanel.add(mTabPanel);
    //    mDockPanel.add(new HTML("Center"));

    //    Window.addResizeHandler(this);
    //    onResize(null);

    final Timer refreshTimer = new Timer() {

      @Override
      public void run() {
        refreshState();
      }
    };
    refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

    mProcessesPanel.start();

    mHistoryHandler = History.addValueChangeHandler(this);

    History.fireCurrentHistoryState();
  }

  /**
   * @category UI
   */
  private ProcessesPanel createProcessesPanel() {
    return new ProcessesPanel(mStatusLabel);
  }

  /**
   * @category UI
   */
  private InstancesPanel createInstancesPanel() {
    return new InstancesPanel(mStatusLabel);
  }

  /**
   * @category UI
   */
  private TasksPanel createTaskPanel() {
    return new TasksPanel(mStatusLabel);
  }

  /**
   * @category method
   */
  protected void refreshState() {

    if (mRefreshCheckbox.getValue()) {
      mProcessesPanel.update();
      mTasksPanel.update();
      mInstancesPanel.update();
    }
  }

  /**
   * Handle history
   *
   * @category action
   */
  @Override
  public void onValueChange(final ValueChangeEvent<String> event) {
    final String value = event.getValue();

    final int c = mTabPanel.getWidgetCount();
    for (int i = 0; i < c; ++i) {
      if (value.equals(mTabPanel.getTabWidget(i).getElement().getInnerText())) {
        mTabPanel.selectTab(i);
        break;
      }
    }

  }

  /**
   * @category action
   */
  @Override
  public void onSelection(final SelectionEvent<Integer> event) {
    if (event.getSource() == mTabPanel) {
      handleTabSelection(event);
    }
  }

  /**
   * @category action
   */
  private void handleTabSelection(final SelectionEvent<Integer> event) {
    final String tabText = mTabPanel.getTabWidget(event.getSelectedItem()).getElement().getInnerText();
    History.newItem(tabText, false);
    if ("Processes".equals(tabText)) {
      mProcessesPanel.start();
    } else {
      mProcessesPanel.stop();
    }
    if ("Instances".equals(tabText)) {
      mInstancesPanel.start();
    } else {
      mInstancesPanel.stop();
    }
    if ("Tasks".equals(tabText)) {
      mTasksPanel.start();
    } else {
      mTasksPanel.stop();
    }
  }

  @Override
  public void onResize(final ResizeEvent event) {
    //    int height = Window.getClientHeight();
    //    mRootPanel.setHeight((height-10)+"px");
    //    height -= 14; // margin
    //    height -= mStatusPanel.getOffsetHeight();
    //    mDockPanel.setHeight(height+"px");
    //    height -= mTabPanel.getTabBar().getOffsetHeight();
    //
    //    height -= 11; // arbitrary missing margin adjustment
    //    mTabPanel.getDeckPanel().setHeight(height+"px");
    //
    //    mProcessesPanel.setHeight(height);
    //    mInstancesPanel.setHeight(height+"px");
    //    mTasksPanel.setHeight(height+"px");
  }

}
