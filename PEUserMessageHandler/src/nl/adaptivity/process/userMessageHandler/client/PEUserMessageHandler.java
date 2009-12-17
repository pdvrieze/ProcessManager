package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PEUserMessageHandler implements EntryPoint, ValueChangeHandler<String>, SelectionHandler<Integer>, ResizeHandler {

  static final String BASEURL = ""/*"http://localhost:8192/ProcessEngine/"*/;

  private static final int REFRESH_INTERVAL = 2000;

  private static final Boolean DEFAULT_REFRESH = false;

  private Label aStatusLabel;

  private CheckBox aRefreshCheckbox;

  private TabLayoutPanel aTabPanel;

  @SuppressWarnings("unused")
  private HandlerRegistration aHistoryHandler;

  private ProcessesPanel aProcessesPanel;

  private InstancesPanel aInstancesPanel;

  private TasksPanel aTasksPanel;

  private RootLayoutPanel aRootPanel;

  private DockLayoutPanel aDockPanel;

  private FlowPanel aStatusPanel;

  /**
   * This is the entry point method.
   * @category UI
   */
  public void onModuleLoad() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      History.newItem("Processes");
    }

    aRootPanel = RootLayoutPanel.get();

    aDockPanel = new DockLayoutPanel(Unit.PX);
    aDockPanel.addNorth(new HTML("<h1 class=\"title\">Process Engine Interface</h1>"), 25);
//    aDockPanel.addStyleName("dockPanel");
    aRootPanel.add(aDockPanel);

    aTabPanel = new TabLayoutPanel(22, Unit.PX);
    aTabPanel.addStyleName("tabPanel");
//    aDockPanel.setCellHeight(aTabPanel, "100%");

    aStatusLabel = new Label();
    aStatusLabel.setText("Initializing...");
    aStatusLabel.addStyleName("statusPanel-left");


    aProcessesPanel = createProcessesPanel();

    aInstancesPanel = createInstancesPanel();

    aTasksPanel = createTaskPanel();
    aTabPanel.add(aProcessesPanel, "Processes");
    aTabPanel.add(aInstancesPanel, "Instances");
    aTabPanel.add(aTasksPanel, "Tasks");
    aTabPanel.selectTab(0);

    aTabPanel.addSelectionHandler(this);

    aStatusPanel = new FlowPanel();
    aStatusPanel.add(aStatusLabel);
    aStatusPanel.addStyleName("statusPanel");

    aRefreshCheckbox = new CheckBox("refresh");
    aRefreshCheckbox.setValue(DEFAULT_REFRESH);
    aRefreshCheckbox.addStyleName("statusPanel-right");
    aStatusPanel.add(aRefreshCheckbox);

    if (! GWT.isScript()) {
      final Label label = new Label("Hosted mode");
      label.addStyleName("span");
      aStatusPanel.add(label);
    } else {
      aStatusPanel.add(new HTML("Status"));
    }

//    aDockPanel.addSouth(new HTML("South"), 20);
    aDockPanel.addSouth(aStatusPanel, 20d);
//    aDockPanel.setCellHeight(aTabPanel, "100%");

    aDockPanel.add(aTabPanel);
//    aDockPanel.add(new HTML("Center"));

//    Window.addResizeHandler(this);
//    onResize(null);

    Timer refreshTimer = new Timer() {
      @Override
      public void run() {
        refreshState();
      }
    };
    refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

    aHistoryHandler = History.addValueChangeHandler(this);

    History.fireCurrentHistoryState();
  }

  /**
   * @category UI
   */
  private ProcessesPanel createProcessesPanel() {
    return new ProcessesPanel(aStatusLabel);
  }

  /**
   * @category UI
   */
  private InstancesPanel createInstancesPanel() {
    return new InstancesPanel(aStatusLabel);
  }

  /**
   * @category UI
   */
  private TasksPanel createTaskPanel() {
    return new TasksPanel(aStatusLabel);
  }

  /**
   * @category method
   */
  protected void refreshState() {

    if (aRefreshCheckbox.getValue()) {
      aProcessesPanel.update();
      aTasksPanel.update();
      aInstancesPanel.update();
    }
  }

  /** Handle history
   * @category action
   */
  @Override
  public void onValueChange(ValueChangeEvent<String> pEvent) {
    final String value = pEvent.getValue();

    int c = aTabPanel.getWidgetCount();
    for(int i = 0; i<c; ++i) {
      if (value.equals(aTabPanel.getTabWidget(i).getElement().getInnerText())) {
        aTabPanel.selectTab(i);
        break;
      }
    }

  }

  /**
   * @category action
   */
  @Override
  public void onSelection(SelectionEvent<Integer> pEvent) {
    if (pEvent.getSource()==aTabPanel) {
      handleTabSelection(pEvent);
    }
  }

  /**
   * @category action
   */
  private void handleTabSelection(SelectionEvent<Integer> pEvent) {
    String tabText = aTabPanel.getTabWidget(pEvent.getSelectedItem()).getElement().getInnerText();
    History.newItem(tabText, false);
    if ("Processes".equals(tabText)) {
      aProcessesPanel.start();
    } else {
      aProcessesPanel.stop();
    }
    if ("Instances".equals(tabText)) {
      aInstancesPanel.start();
    } else {
      aInstancesPanel.stop();
    }
    if ("Tasks".equals(tabText)) {
      aTasksPanel.start();
    } else {
      aTasksPanel.stop();
    }
  }

  @Override
  public void onResize(ResizeEvent pEvent) {
//    int height = Window.getClientHeight();
//    aRootPanel.setHeight((height-10)+"px");
//    height -= 14; // margin
//    height -= aStatusPanel.getOffsetHeight();
//    aDockPanel.setHeight(height+"px");
//    height -= aTabPanel.getTabBar().getOffsetHeight();
//
//    height -= 11; // arbitrary missing margin adjustment
//    aTabPanel.getDeckPanel().setHeight(height+"px");
//
//    aProcessesPanel.setHeight(height);
//    aInstancesPanel.setHeight(height+"px");
//    aTasksPanel.setHeight(height+"px");
  }

}
