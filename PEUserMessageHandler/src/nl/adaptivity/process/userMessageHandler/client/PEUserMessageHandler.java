package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PEUserMessageHandler implements EntryPoint, ValueChangeHandler<String>, SelectionHandler<Integer> {

  static final String BASEURL = ""/*"http://localhost:8192/ProcessEngine/"*/;

  private static final int REFRESH_INTERVAL = 2000;

  private static final Boolean DEFAULT_REFRESH = false;

  private Label aStatusLabel;

  private CheckBox aRefreshCheckbox;

  private TabPanel aTabPanel;

  @SuppressWarnings("unused")
  private HandlerRegistration aHistoryHandler;

  private ProcessesPanel aProcessesPanel;

  private InstancesPanel aInstancesPanel;

  private TasksPanel aTasksPanel;

  /**
   * This is the entry point method.
   * @category UI
   */
  public void onModuleLoad() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      History.newItem("Processes");
    }

    final RootPanel rootPanel = RootPanel.get("gwt");

    DockPanel dockPanel = new DockPanel();
    rootPanel.add(dockPanel);

    aTabPanel = new TabPanel();
    dockPanel.add(aTabPanel, DockPanel.CENTER);

    aStatusLabel = new Label();
    aStatusLabel.setText("Initializing...");

    aProcessesPanel = createProcessesPanel();

    aInstancesPanel = createInstancesPanel();

    aTasksPanel = createTaskPanel();

    aTabPanel.add(aProcessesPanel, "Processes");
    aTabPanel.add(aInstancesPanel, "Instances");
    aTabPanel.add(aTasksPanel, "Tasks");
    aTabPanel.selectTab(0);

    aTabPanel.getTabBar().addSelectionHandler(this);

    DockPanel statusPanel = new DockPanel();
    statusPanel.add(aStatusLabel, DockPanel.WEST);

    aRefreshCheckbox = new CheckBox("refresh");
    aRefreshCheckbox.setValue(DEFAULT_REFRESH);
    statusPanel.add(aRefreshCheckbox, DockPanel.EAST);
    statusPanel.addStyleName("fullWidth");
    dockPanel.add(statusPanel, DockPanel.SOUTH);

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

    int c = aTabPanel.getTabBar().getTabCount();
    for(int i = 0; i<c; ++i) {
      if (value.equals(aTabPanel.getTabBar().getTabHTML(i))) {
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
    if (pEvent.getSource()==aTabPanel.getTabBar()) {
      handleTabSelection(pEvent);
    }
  }

  /**
   * @category action
   */
  private void handleTabSelection(SelectionEvent<Integer> pEvent) {
    String tabText = aTabPanel.getTabBar().getTabHTML(pEvent.getSelectedItem());
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

}
