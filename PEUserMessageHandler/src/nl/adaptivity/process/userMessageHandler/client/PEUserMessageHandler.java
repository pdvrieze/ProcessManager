package nl.adaptivity.process.userMessageHandler.client;

import java.util.ArrayList;

import nl.adaptivity.gwt.base.client.MyFileUpload;
import nl.adaptivity.gwt.base.client.MyFormPanel;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteEvent;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteHandler;
import nl.adaptivity.gwt.ext.client.ControllingListBox;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PEUserMessageHandler implements EntryPoint, ClickHandler, ChangeHandler {

  private final class FileSubmitHandler implements SubmitCompleteHandler {

    @Override
    public void onSubmitComplete(SubmitCompleteEvent pEvent) {
      aStatusLabel.setText("File submit complete!!");
      com.google.gwt.dom.client.Document results = pEvent.getResults();
      if (results != null) {
        GWT.log("Actually got a response: \""+results+"\"", null);
        updateProcessList(asProcessList(results));
      } else {
        requestProcessesUpdate();
      }
    }
  }

  private final class ProcessListCallback implements RequestCallback {

    @Override
    public void onError(Request pRequest, Throwable pException) {
      aStatusLabel.setText("Error: "+pException.getMessage());
    }

    @Override
    public void onResponseReceived(Request pRequest, Response pResponse) {
      if (200 == pResponse.getStatusCode()) {
        updateProcessList(asProcessList(pResponse.getText()));
        aStatusLabel.setText("Ok");
      } else {
        aStatusLabel.setText("Error("+pResponse.getStatusCode()+"): "+pResponse.getStatusText());
      }
    }
  }

  private final class TaskListCallback implements RequestCallback {

    @Override
    public void onError(Request pRequest, Throwable pException) {
      aStatusLabel.setText("Error: "+pException.getMessage());
    }

    @Override
    public void onResponseReceived(Request pRequest, Response pResponse) {
      // TODO implement
//      if (200 == pResponse.getStatusCode()) {
//        updateProcessList(asProcessList(pResponse.getText()));
//        aStatusLabel.setText("Ok");
//      } else {
//        aStatusLabel.setText("Error("+pResponse.getStatusCode()+"): "+pResponse.getStatusText());
//      }
    }
  }

  private final class InstanceListCallback implements RequestCallback {

    @Override
    public void onError(Request pRequest, Throwable pException) {
      aStatusLabel.setText("Error: "+pException.getMessage());
    }

    @Override
    public void onResponseReceived(Request pRequest, Response pResponse) {
      // TODO implement
//      if (200 == pResponse.getStatusCode()) {
//        updateProcessList(asProcessList(pResponse.getText()));
//        aStatusLabel.setText("Ok");
//      } else {
//        aStatusLabel.setText("Error("+pResponse.getStatusCode()+"): "+pResponse.getStatusText());
//      }
    }
  }

  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while " + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  private static final String BASEURL = "/ProcessEngine/"/*"http://localhost:8192/ProcessEngine/"*/;

  private static final String PROCESSLISTURL = BASEURL+"processModels";

  private static final String PROCESSINSTANCELISTURL = BASEURL+"processInstances";

  private static final String TASKLISTURL = BASEURL+"tasks";

  private static final int REFRESH_INTERVAL = 2000;

  private static final Boolean DEFAULT_REFRESH = false;

  private Button aStartTaskButton;

  private Button aShowInstanceStatusButton;

  private Button aStartProcessButton;

  private Label aStatusLabel;

  private ControllingListBox aProcessListBox;

  private ControllingListBox aInstanceListBox;

  private ControllingListBox aTaskListBox;

  private MyFormPanel aProcessFileForm;

  private Button aProcessFileSubmitButton;

  private MyFileUpload aProcessUpload;

  private final ProcessListCallback aProcessListCallback = new ProcessListCallback();

  private final InstanceListCallback aInstanceListCallback = new InstanceListCallback();

  private final TaskListCallback aTaskListCallback = new TaskListCallback();

  private CheckBox aRefreshCheckbox;

  private TabPanel aTabPanel;

  /**
   * @category helper
   */
  private static ProcessModelRef[] asProcessList(final String pText) {
    final Document myResponse;
    
    myResponse = XMLParser.parse(pText);
    ArrayList<ProcessModelRef> result = new ArrayList<ProcessModelRef>();
    
    Node root = myResponse.getFirstChild();
    if (root.getNodeName().equals("processModels")) {
      Node child = root.getFirstChild();
      while(child!=null) {
        if ("processModel".equals(child.getNodeName()) ){
          final NamedNodeMap attributes = child.getAttributes();
          String name = attributes.getNamedItem("name").getNodeValue();
          String handle = attributes.getNamedItem("handle").getNodeValue();
          result.add(new ProcessModelRef(handle, name));
        }
        child = child.getNextSibling();
      }
      return result.toArray(new ProcessModelRef[result.size()]);
    }
    
    return new ProcessModelRef[0];
  }

  /**
   * @category helper
   */
  private static ProcessModelRef[] asProcessList(final com.google.gwt.dom.client.Document pDocument) {
    ArrayList<ProcessModelRef> result = new ArrayList<ProcessModelRef>();
    
    Element root = Element.as(pDocument.getFirstChild());
    if (root.getNodeName().equals("processModels")) {
      Element child = root.getFirstChildElement();
      while(child!=null) {
        if ("processModel".equals(child.getNodeName()) ){
          String name = child.getAttribute("name");
          String handle = child.getAttribute("handle");
          result.add(new ProcessModelRef(handle, name));
        }
        child = child.getNextSiblingElement();
      }
      GWT.log("  return "+result.toString(), null);
      return result.toArray(new ProcessModelRef[result.size()]);
    }
    
    return new ProcessModelRef[0];
  }

  /**
   * This is the entry point method.
   * @category UI
   */
  public void onModuleLoad() {
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
    
    DockPanel statusPanel = new DockPanel();
    aStatusLabel = new Label();
    aStatusLabel.setText("Initializing...");
    statusPanel.add(aStatusLabel, DockPanel.WEST);
    
    aRefreshCheckbox = new CheckBox("refresh");
    aRefreshCheckbox.setValue(DEFAULT_REFRESH);
    statusPanel.add(aRefreshCheckbox, DockPanel.EAST);
    statusPanel.addStyleName("fullWidth");
    dockPanel.add(statusPanel, DockPanel.SOUTH);
    
    requestProcessesUpdate();
    
    Timer refreshTimer = new Timer() {
      @Override
      public void run() {
        refreshState();
      }
    };
    refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
  }

  /**
   * @category UI
   */
  private HorizontalPanel createProcessesPanel() {
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");
    
    aProcessListBox = new ControllingListBox();
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
    
    aInstanceListBox = new ControllingListBox();
    hp1.add(aInstanceListBox);
    aInstanceListBox.addStyleName("mhList");
    aInstanceListBox.addStyleName("tabContent");
    aInstanceListBox.addChangeHandler(this);
    
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
    
    
    aTaskListBox = new ControllingListBox();
    aTaskListBox.addStyleName("mhList");
    aTaskListBox.addStyleName("tabContent");
    hp1.add(aTaskListBox);
    aTaskListBox.addChangeHandler(this);
    
    VerticalPanel vp1 = new VerticalPanel();
    hp1.add(vp1);
    vp1.addStyleName("tabContent");
    
    aStartTaskButton = new Button("Start task");
    aTaskListBox.addControlledWidget(aStartTaskButton);
    aStartTaskButton.addStyleName("inTabButton");
    vp1.add(aStartTaskButton);
    aStartTaskButton.addClickHandler(this);
    return hp1;
  }

  /**
     * @category UI 
     */
    private void updateProcessList(ProcessModelRef[] pProcessModels) {
      int selectedIndex = aProcessListBox.getSelectedIndex();
      String selected = selectedIndex>=0 ? aProcessListBox.getValue(selectedIndex) : null;
      aProcessListBox.clear();
      
      int newSelected = -1;
      int i=0;
      for(ProcessModelRef ref:pProcessModels) {
        aProcessListBox.addItem(ref.name, ref.handle);
        if (ref.handle.equals(selected)) {
          newSelected = i;
        }
        ++i;
      }
      if (newSelected>=0) {
        aProcessListBox.setSelectedIndex(newSelected);
      }
    }

  /**
   * @category method
   */
  private void requestProcessesUpdate() {
    RequestBuilder rBuilder = new RequestBuilder(RequestBuilder.GET, PROCESSLISTURL);
    
    try {
      rBuilder.sendRequest(null, aProcessListCallback);
    } catch (RequestException e) {
      aStatusLabel.setText("Error: "+e.getMessage());
    }
  }

  /**
   * @category method
   */
  private void requestInstancesUpdate() {
    GWT.log("requestInstancesUpdate called",null);
    RequestBuilder rBuilder = new RequestBuilder(RequestBuilder.GET, PROCESSINSTANCELISTURL);
    
    try {
      rBuilder.sendRequest(null, aInstanceListCallback);
    } catch (RequestException e) {
      aStatusLabel.setText("Error: "+e.getMessage());
    }
  }

  /**
   * @category method
   */
  private void requestTaskUpdate() {
    GWT.log("requestProcessesUpdate called",null);
    RequestBuilder rBuilder = new RequestBuilder(RequestBuilder.GET, TASKLISTURL);
    
    try {
      rBuilder.sendRequest(null, aTaskListCallback);
    } catch (RequestException e) {
      aStatusLabel.setText("Error: "+e.getMessage());
    }
  }
  
  /**
   * @category method
   */
  protected void refreshState() {
    
    if (aRefreshCheckbox.getValue()) {
      
      requestProcessesUpdate();
    }
//    requestInstancesUpdate();
//    requestTaskUpdate();
  }

  /**
   * @category event handler
   */
  @Override
  public void onClick(ClickEvent pEvent) {
    if (pEvent.getSource()==aStartTaskButton) {
      startTask(pEvent);
    } else if (pEvent.getSource()==aShowInstanceStatusButton) {
      showInstance(pEvent);
    } else if (pEvent.getSource()==aStartProcessButton) {
      startProcess(pEvent);
    } else if (pEvent.getSource()==aProcessFileSubmitButton) {
      submitProcessFile(pEvent);
    }
  }

  /**
   * @category event handler
   */
  @Override
  public void onChange(ChangeEvent pEvent) {
    if (pEvent.getSource()==aProcessUpload) {
      changeProcessUpload(pEvent);
    }
  }

  /**
   * @category event handler
   */
  private void changeProcessUpload(ChangeEvent pEvent) {
    aProcessFileSubmitButton.setEnabled(aProcessUpload.getFilename().length()>0);
    aStatusLabel.setText("upload file changed");
  }

  /**
   * @category action
   */
  private void submitProcessFile(ClickEvent pEvent) {
    aProcessFileForm.addSubmitCompleteHandler(new FileSubmitHandler());
    aProcessFileForm.submit();
    
    aProcessFileForm.reset();
  }

  /**
   * @category action
   */
  private void startProcess(ClickEvent pEvent) {
    aStatusLabel.setText("startProcess");
  }

  /**
   * @category action
   */
  private void showInstance(DomEvent<?> pEvent) {
    aStatusLabel.setText("showInstance");
  }

  /**
   * @category action
   */
  private void startTask(DomEvent<?> pEvent) {
    aStatusLabel.setText("startTask");
  }

}
