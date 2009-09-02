package nl.adaptivity.process.userMessageHandler.client;

import java.util.ArrayList;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class PEUserMessageHandler implements EntryPoint, ClickHandler, ChangeHandler {

  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while " + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  private static final String BASEURL = "/peusermessagehandler/rest/"/*"http://localhost:8192/ProcessEngine/"*/;

  private static final String PROCESSLISTURL = BASEURL+"processModels";

  /**
   * Create a remote service proxy to talk to the server-side Greeting service.
   */
  private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);

  private Button aStartTaskButton;

  private Button aShowInstanceStatusButton;

  private Button aStartProcessButton;

  private Label aStatusLabel;

  private ListBox aProcessListBox;

  private ListBox aInstanceListBox;

  private ListBox aTaskListBox;

  private FormPanel aProcessFileForm;

  private Button aProcessFileSubmitButton;

  private MyFileUpload aProcessUpload;

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    final RootPanel rootPanel = RootPanel.get("gwt");
    
    DockPanel dockPanel = new DockPanel();
    rootPanel.add(dockPanel);
    
    TabPanel tabPanel = new TabPanel();
    dockPanel.add(tabPanel, DockPanel.CENTER);
    
    HorizontalPanel hp1 = createProcessesPanel();
    
    HorizontalPanel hp2 = createInstancesPanel();
    
    HorizontalPanel hp3 = createTaskPanel();
    
    tabPanel.add(hp1, "Processes");
    tabPanel.add(hp2, "Instances");
    tabPanel.add(hp3, "Tasks");
    tabPanel.selectTab(0);
    
    
    aStatusLabel = new Label();
    aStatusLabel.setText("Initializing...");
    dockPanel.add(aStatusLabel, DockPanel.SOUTH);
    
    requestProcessesUpdate();
  }

  private void requestProcessesUpdate() {
    RequestBuilder rBuilder = new RequestBuilder(RequestBuilder.GET, PROCESSLISTURL);
    
    try {
      Request request = rBuilder.sendRequest(null, new RequestCallback() {

        @Override
        public void onError(Request pRequest, Throwable pException) {
          aStatusLabel.setText("Error: "+pException.getMessage());
        }

        @Override
        public void onResponseReceived(Request pRequest, Response pResponse) {
          if (200 == pResponse.getStatusCode()) {
            updateProcessList(asProcessList(pResponse));
            aStatusLabel.setText("Ok");
          } else {
            aStatusLabel.setText("Error("+pResponse.getStatusCode()+"): "+pResponse.getStatusText());
          }
        }
        
      });
    } catch (RequestException e) {
      aStatusLabel.setText("Error: "+e.getMessage());
    }

    
  }

  private void updateProcessList(ProcessModelRef[] pProcessModels) {
    GWT.log("updateProcessList", null);
//    int selectedIndex = aProcessListBox.getSelectedIndex();
//    String selected = aProcessListBox.getItemText(selectedIndex);
    aProcessListBox.clear();
    
//    int newSelected = -1;
//    int i=0;
    for(ProcessModelRef ref:pProcessModels) {
      aProcessListBox.addItem(ref.name, Long.toString(ref.handle));
//      if (Long.toString(ref.handle).equals(selected)) {
//        newSelected = i;
//      }
//      ++i;
    }
//    if (newSelected>=0) {
//      aProcessListBox.setSelectedIndex(newSelected);
//    }
  }

  private ProcessModelRef[] asProcessList(Response pResponse) {
    Document myResponse = XMLParser.parse(pResponse.getText());
    ArrayList<ProcessModelRef> result = new ArrayList<ProcessModelRef>();
    
    Node root = myResponse.getFirstChild();
    if (root.getNodeName().equals("processModels")) {
      Node child = root.getFirstChild();
      while(child!=null) {
        if ("processModel".equals(child.getNodeName()) ){
          final NamedNodeMap attributes = child.getAttributes();
          String name = attributes.getNamedItem("name").getNodeValue();
          long handle = Long.parseLong(attributes.getNamedItem("handle").getNodeValue());
          result.add(new ProcessModelRef(handle, name));
        }
        child = child.getNextSibling();
      }
      return result.toArray(new ProcessModelRef[result.size()]);
    }
    
    return new ProcessModelRef[0];
  }

  private HorizontalPanel createProcessesPanel() {
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");
    
    aProcessListBox = new ListBox();
    aProcessListBox.setVisibleItemCount(10);
    hp1.add(aProcessListBox);
    
    aProcessListBox.addItem("Process1");
    aProcessListBox.addItem("Process2");
    aProcessListBox.addStyleName("mhList");
    aProcessListBox.addStyleName("tabContent");
    aProcessListBox.addChangeHandler(this);
    
    VerticalPanel vp1 = new VerticalPanel();
    vp1.addStyleName("tabContent");
    hp1.add(vp1);
    
    aStartProcessButton = new Button("Start process");
    aStartProcessButton.setEnabled(false);
    aStartProcessButton.addStyleName("inTabButton");
    aStartProcessButton.addClickHandler(this);
    vp1.add(aStartProcessButton);
    
    aProcessFileForm = new FormPanel();
    aProcessFileForm.setAction("uploadProcess");
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
    aProcessUpload.addChangeHandler(this);
    vp2.add(aProcessUpload);
    
    
    aProcessFileSubmitButton = new Button("Submit");
    aProcessFileSubmitButton.addClickHandler(this);
    aProcessFileSubmitButton.setEnabled(false);
    vp2.add(aProcessFileSubmitButton);
    
    vp1.add(aProcessFileForm);
    return hp1;
  }

  private HorizontalPanel createInstancesPanel() {
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");
    
    aInstanceListBox = new ListBox();
    aInstanceListBox.setVisibleItemCount(10);
    hp1.add(aInstanceListBox);
    aInstanceListBox.addStyleName("mhList");
    aInstanceListBox.addStyleName("tabContent");
    aInstanceListBox.addChangeHandler(this);
    
    VerticalPanel vp1 = new VerticalPanel();
    hp1.add(vp1);
    vp1.addStyleName("tabContent");
    
    aShowInstanceStatusButton = new Button("Show status");
    aShowInstanceStatusButton.setEnabled(false);
    aShowInstanceStatusButton.addStyleName("inTabButton");
    vp1.add(aShowInstanceStatusButton);
    aShowInstanceStatusButton.addClickHandler(this);
    return hp1;
  }


  private HorizontalPanel createTaskPanel() {
    HorizontalPanel hp1 = new HorizontalPanel();
    hp1.addStyleName("tabPanel");
    
    
    aTaskListBox = new ListBox();
    aTaskListBox.setVisibleItemCount(10);
    aTaskListBox.addStyleName("mhList");
    aTaskListBox.addStyleName("tabContent");
    hp1.add(aTaskListBox);
    aTaskListBox.addChangeHandler(this);
    
    VerticalPanel vp1 = new VerticalPanel();
    hp1.add(vp1);
    vp1.addStyleName("tabContent");
    
    aStartTaskButton = new Button("Start task");
    aStartTaskButton.setEnabled(false);
    aStartTaskButton.addStyleName("inTabButton");
    vp1.add(aStartTaskButton);
    aStartTaskButton.addClickHandler(this);
    return hp1;
  }

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

  private void submitProcessFile(ClickEvent pEvent) {
    // aProcessFileForm.submit();
    
    aProcessFileForm.reset();
    
    
    // TODO Auto-generated method stub
    // 
    throw new UnsupportedOperationException("Not yet implemented");
    
  }

  private void startProcess(ClickEvent pEvent) {
    aStatusLabel.setText("startProcess");
  }

  private void showInstance(DomEvent<?> pEvent) {
    aStatusLabel.setText("showInstance");
  }

  private void startTask(DomEvent<?> pEvent) {
    aStatusLabel.setText("startTask");
  }

  @Override
  public void onChange(ChangeEvent pEvent) {
    if (pEvent.getSource()==aProcessListBox) {
      changeProcessList(pEvent);
    } else if (pEvent.getSource()==aInstanceListBox) {
      changeInstanceList(pEvent);
    } else if (pEvent.getSource()==aTaskListBox) {
      changeTaskList(pEvent);
    } else if (pEvent.getSource()==aProcessUpload) {
      changeProcessUpload(pEvent);
    }
  }

  private void changeProcessUpload(ChangeEvent pEvent) {
    aProcessFileSubmitButton.setEnabled(aProcessUpload.getFilename().length()>0);
    aStatusLabel.setText("upload file changed");
  }

  private void changeTaskList(ChangeEvent pEvent) {
    aStartTaskButton.setEnabled(aTaskListBox.getSelectedIndex()>=0);
  }

  private void changeInstanceList(ChangeEvent pEvent) {
    aShowInstanceStatusButton.setEnabled(aInstanceListBox.getSelectedIndex()>=0);
  }

  private void changeProcessList(ChangeEvent pEvent) {
    aStartProcessButton.setEnabled(aProcessListBox.getSelectedIndex()>=0);
  }

}
