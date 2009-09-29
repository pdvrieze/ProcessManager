package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.base.client.MyFileUpload;
import nl.adaptivity.gwt.base.client.MyFormPanel;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteEvent;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteHandler;
import nl.adaptivity.gwt.ext.client.RemoteListBox;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PEUserMessageHandler implements EntryPoint, ClickHandler, ChangeHandler, ValueChangeHandler<String>, SelectionHandler<Integer> {

  private final class FileSubmitHandler implements SubmitCompleteHandler {

    @Override
    public void onSubmitComplete(SubmitCompleteEvent pEvent) {
      aStatusLabel.setText("File submit complete!!");
      com.google.gwt.dom.client.Document results = pEvent.getResults();
      if (results != null) {
        aProcessListBox.update(results);
      } else {
        aProcessListBox.update();
      }
    }
  }

  private static final String BASEURL = ""/*"http://localhost:8192/ProcessEngine/"*/;

  private static final String PROCESSLISTURL = BASEURL+"/ProcessEngine/processModels";

  private static final String PROCESSINSTANCELISTURL = BASEURL+"/ProcessEngine/processInstances";

  private static final String TASKLISTURL = BASEURL+"/UserMessageService/pendingTasks";

  private static final int REFRESH_INTERVAL = 2000;

  private static final Boolean DEFAULT_REFRESH = false;

  private Button aStartTaskButton;
  private Button aTakeTaskButton;
  private Button aCompleteTaskButton;

  private Button aShowInstanceStatusButton;

  private Button aStartProcessButton;

  private Label aStatusLabel;

  private RemoteListBox aProcessListBox;

  private RemoteListBox aInstanceListBox;

  private RemoteListBox aTaskListBox;

  private MyFormPanel aProcessFileForm;

  private Button aProcessFileSubmitButton;

  private MyFileUpload aProcessUpload;

  private CheckBox aRefreshCheckbox;

  private TabPanel aTabPanel;

  @SuppressWarnings("unused")
  private HandlerRegistration aHistoryHandler;

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

    HorizontalPanel hp1 = createProcessesPanel();

    HorizontalPanel hp2 = createInstancesPanel();

    HorizontalPanel hp3 = createTaskPanel();

    aTabPanel.add(hp1, "Processes");
    aTabPanel.add(hp2, "Instances");
    aTabPanel.add(hp3, "Tasks");
    aTabPanel.selectTab(0);

    aTabPanel.getTabBar().addSelectionHandler(this);

    DockPanel statusPanel = new DockPanel();
    aStatusLabel = new Label();
    aStatusLabel.setText("Initializing...");
    statusPanel.add(aStatusLabel, DockPanel.WEST);

    aRefreshCheckbox = new CheckBox("refresh");
    aRefreshCheckbox.setValue(DEFAULT_REFRESH);
    statusPanel.add(aRefreshCheckbox, DockPanel.EAST);
    statusPanel.addStyleName("fullWidth");
    dockPanel.add(statusPanel, DockPanel.SOUTH);

    aProcessListBox.start();
    aTaskListBox.start();

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
  private HorizontalPanel createProcessesPanel() {
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");

    aProcessListBox = new RemoteListBox(PROCESSLISTURL);
    aProcessListBox.setRootElement("processModels");
    aProcessListBox.setListElement("processModel");
    aProcessListBox.setValueElement("@handle");
    aProcessListBox.setTextElement("=@{handle}: @{name}");
    hp1.add(aProcessListBox);

    aProcessListBox.addItem("Process1");
    aProcessListBox.addItem("Process2");
    aProcessListBox.addStyleName("mhList");
    aProcessListBox.addStyleName("tabContent");

    VerticalPanel vp1 = new VerticalPanel();
    vp1.addStyleName("tabContent");
    hp1.add(vp1);

    aStartProcessButton = new Button("Start process");
    aProcessListBox.addControlledWidget(aStartProcessButton);
    aStartProcessButton.addStyleName("inTabButton");
    aStartProcessButton.addClickHandler(this);
    vp1.add(aStartProcessButton);

    aProcessFileForm = new MyFormPanel();
    aProcessFileForm.setAction(PROCESSLISTURL);
    aProcessFileForm.setEncoding(FormPanel.ENCODING_MULTIPART);
    aProcessFileForm.setMethod(FormPanel.METHOD_POST);
    aProcessFileForm.addStyleName("fileForm");

    VerticalPanel vp2 = new VerticalPanel();
    aProcessFileForm.setWidget(vp2);

    Label label = new Label();
    label.setText("Upload new model");
    vp2.add(label);

    aProcessUpload = new MyFileUpload();
    aProcessUpload.setName("processUpload");
    aProcessUpload.registerChangeHandler(this);
    vp2.add(aProcessUpload);


    aProcessFileSubmitButton = new Button("Submit");
    aProcessFileSubmitButton.addClickHandler(this);
    aProcessUpload.addControlledWidget(aProcessFileSubmitButton);
    vp2.add(aProcessFileSubmitButton);

    vp1.add(aProcessFileForm);
    return hp1;
  }

  /**
   * @category UI
   */
  private HorizontalPanel createInstancesPanel() {
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");

    aInstanceListBox = new RemoteListBox(PROCESSINSTANCELISTURL);
    aInstanceListBox.setRootElement("processInstances");
    aInstanceListBox.setListElement("processInstance");
    aInstanceListBox.setValueElement("@handle");
    aInstanceListBox.setTextElement("=Instance @{handle} of model (@{processModel})");


    hp1.add(aInstanceListBox);
    aInstanceListBox.addStyleName("mhList");
    aInstanceListBox.addStyleName("tabContent");

    VerticalPanel vp1 = new VerticalPanel();
    hp1.add(vp1);
    vp1.addStyleName("tabContent");

    aShowInstanceStatusButton = new Button("Show status");
    aInstanceListBox.addControlledWidget(aShowInstanceStatusButton);
    aShowInstanceStatusButton.addStyleName("inTabButton");
    vp1.add(aShowInstanceStatusButton);
    aShowInstanceStatusButton.addClickHandler(this);
    return hp1;
  }

  /**
   * @category UI
   */
  private HorizontalPanel createTaskPanel() {
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");


    aTaskListBox = new RemoteListBox(TASKLISTURL);
    aTaskListBox.addStyleName("mhList");
    aTaskListBox.addStyleName("tabContent");
    aTaskListBox.setRootElement("tasks");
    aTaskListBox.setTextElement("=@summary (@{state})");
    aTaskListBox.setValueElement("@handle");
    aTaskListBox.setListElement("task");

    hp1.add(aTaskListBox);
//    aTaskListBox.addChangeHandler(this);

    VerticalPanel vp1 = new VerticalPanel();
    hp1.add(vp1);
    vp1.addStyleName("tabContent");


    aTakeTaskButton = new Button("Take task");
    aTaskListBox.addControlledWidget(aTakeTaskButton);
    aTakeTaskButton.addStyleName("inTabButton");
    vp1.add(aTakeTaskButton);
    aTakeTaskButton.addClickHandler(this);

    aStartTaskButton = new Button("Start task");
    aTaskListBox.addControlledWidget(aStartTaskButton);
    aStartTaskButton.addStyleName("inTabButton");
    vp1.add(aStartTaskButton);
    aStartTaskButton.addClickHandler(this);

    aCompleteTaskButton = new Button("Complete task");
    aTaskListBox.addControlledWidget(aCompleteTaskButton);
    aCompleteTaskButton.addStyleName("inTabButton");
    vp1.add(aCompleteTaskButton);
    aCompleteTaskButton.addClickHandler(this);


    return hp1;
  }

  /**
   * @category method
   */
  protected void refreshState() {

    if (aRefreshCheckbox.getValue()) {
      aProcessListBox.update();
      aTaskListBox.update();
      aInstanceListBox.update();
    }
  }

  /**
   * @category event handler
   */
  @Override
  public void onClick(ClickEvent pEvent) {
    if (pEvent.getSource()==aStartTaskButton) {
      startTask();
    } else if (pEvent.getSource()==aTakeTaskButton){
      takeTask();
    } else if (pEvent.getSource()==aCompleteTaskButton){
      completeTask();
    } else if (pEvent.getSource()==aShowInstanceStatusButton) {
      showInstance();
    } else if (pEvent.getSource()==aStartProcessButton) {
      startProcess();
    } else if (pEvent.getSource()==aProcessFileSubmitButton) {
      submitProcessFile();
    }
  }

  /**
   * @category event handler
   */
  @Override
  public void onChange(ChangeEvent pEvent) {
    if (pEvent.getSource()==aProcessUpload) {
      changeProcessUpload();
    }
  }

  /**
   * @category event handler
   */
  private void changeProcessUpload() {
    aProcessFileSubmitButton.setEnabled(aProcessUpload.getFilename().length()>0);
    aStatusLabel.setText("upload file changed");
  }

  /**
   * @category action
   */
  private void submitProcessFile() {
    aProcessFileForm.addSubmitCompleteHandler(new FileSubmitHandler());
    aProcessFileForm.submit();

    aProcessFileForm.reset();
  }

  /**
   * @category action
   */
  private void startProcess() {
    aStatusLabel.setText("startProcess");
    String handle = aProcessListBox.getValue(aProcessListBox.getSelectedIndex());
    String URL=PROCESSLISTURL+"/"+handle;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, URL);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
    String postData = "op=newInstance";

    try {
      rb.sendRequest(postData, new RequestCallback() {

        @Override
        public void onError(Request pRequest, Throwable pException) {
          aStatusLabel.setText("Error ("+pException.getMessage()+")");
        }

        @Override
        public void onResponseReceived(Request pRequest, Response pResponse) {
          aStatusLabel.setText("Process instantiated");
          // TODO perhaps do something with this, but the instances are not visible from the tab
//          aInstanceListBox.update();
        }

      });
    } catch (RequestException e) {
      aStatusLabel.setText("Error ("+e.getMessage()+")");
    }

  }

  /**
   * @category action
   */
  private void showInstance() {
    aStatusLabel.setText("showInstance");
  }

  /**
   * @category action
   */
  private void startTask() {
    aStatusLabel.setText("startTask");
    String newState = "Started";
    updateTaskState(newState, aTaskListBox.getValue(aTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void takeTask() {
    aStatusLabel.setText("takeTask");
    String newState = "Taken";
    updateTaskState(newState, aTaskListBox.getValue(aTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void completeTask() {
    aStatusLabel.setText("completeTask");
    String newState = "Finished";
    updateTaskState(newState, aTaskListBox.getValue(aTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void updateTaskState(String newState, String handle) {
    String URL=TASKLISTURL+"/"+handle;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, URL);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
    String postData = "state="+newState;

    try {
      rb.sendRequest(postData, new RequestCallback() {

        @Override
        public void onError(Request pRequest, Throwable pException) {
          aStatusLabel.setText("Error ("+pException.getMessage()+")");
        }

        @Override
        public void onResponseReceived(Request pRequest, Response pResponse) {
          aTaskListBox.update();
        }

      });
    } catch (RequestException e) {
      aStatusLabel.setText("Error ("+e.getMessage()+")");
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
      aProcessListBox.start();
    } else {
      aProcessListBox.stop();
    }
    if ("Instances".equals(tabText)) {
      aInstanceListBox.start();
    } else {
      aInstanceListBox.stop();
    }
    if ("Tasks".equals(tabText)) {
      aTaskListBox.start();
    } else {
      aTaskListBox.stop();
    }
  }

}
