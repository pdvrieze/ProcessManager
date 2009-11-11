package nl.adaptivity.process.userMessageHandler.client;

import java.util.ArrayList;

import nl.adaptivity.gwt.base.client.MyFileUpload;
import nl.adaptivity.gwt.base.client.MyFormPanel;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteEvent;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteHandler;
import nl.adaptivity.gwt.ext.client.RemoteListBox;
import nl.adaptivity.gwt.ext.client.TextInputPopup;
import nl.adaptivity.gwt.ext.client.TextInputPopup.InputCompleteEvent;
import nl.adaptivity.gwt.ext.client.TextInputPopup.InputCompleteHandler;
import nl.adaptivity.process.userMessageHandler.client.processModel.ProcessModel;
import nl.adaptivity.process.userMessageHandler.client.processModel.ProcessNode;
import nl.adaptivity.process.userMessageHandler.client.processModel.StartNode;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.*;


public class ProcessesPanel extends LayoutComposite implements ClickHandler, ChangeHandler {

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

  private static final String PROCESSLISTURL = PEUserMessageHandler.BASEURL+"/ProcessEngine/processModels";

  private Button aStartProcessButton;
  private RemoteListBox aProcessListBox;
  private MyFormPanel aProcessFileForm;
  private Button aProcessFileSubmitButton;
  private MyFileUpload aProcessUpload;

  private final Label aStatusLabel;

  private VerticalPanel aFormPanel;

  private Button aRenameProcessButton;

  private VerticalPanel aLowerPanel;

  private SplittedFillLeftPanel<Widget> aRoot;

  private Button aEditProcessButton;

  private ProcessEditPanel aProcessEditPanel;

  private Button aNewProcessButton;

  public ProcessesPanel(Label pStatusLabel) {
    aRoot = new SplittedFillLeftPanel<Widget>();
    aRoot.setTopLeftWidget(new HTML("Top Left"));
    aRoot.setRightWidget(new HTML("Right"));

//    HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();

    aStatusLabel = pStatusLabel;

    initListBox();

    initLowerPanel();

    aProcessEditPanel = new ProcessEditPanel(false);
    aProcessEditPanel.setInstance(false);
    aRoot.setRightWidget(aProcessEditPanel);

    initWidget(aRoot);
  }


  private void initListBox() {
    aProcessListBox = new RemoteListBox(PROCESSLISTURL);
    aProcessListBox.setRootElement("processModels");
    aProcessListBox.setListElement("processModel");
    aProcessListBox.setValueElement("@handle");
    aProcessListBox.setTextElement("=@{handle}: @{name}");
    aRoot.setTopLeftWidget(aProcessListBox);

    aProcessListBox.addStyleName("mhList");
    aProcessListBox.addStyleName("tabContent");
  }


  private void initLowerPanel() {
    aLowerPanel = new VerticalPanel();
    aLowerPanel.addStyleName("tabContent");
    aRoot.setBottomLeftWidget(aLowerPanel);

    aStartProcessButton = createLeftButton("Start process", true);
    aNewProcessButton = createLeftButton("New process", false);
    aEditProcessButton = createLeftButton("View process", true);
    aRenameProcessButton = createLeftButton("Rename process", true);

    initUploadPanel();

    aLowerPanel.add(aProcessFileForm);

    aProcessUpload.setWidth("100%");
    aProcessFileForm.setWidth("100%");
    aFormPanel.setWidth("100%");
  }


  private Button createLeftButton(String pCaption, boolean pControlled) {
    Button result = new Button(pCaption);
    if (pControlled) {
      aProcessListBox.addControlledWidget(result);
    }
    result.addStyleName("inTabButton");
    result.addClickHandler(this);
    aLowerPanel.add(result);
    return result;
  }


  private void initUploadPanel() {
    aProcessFileForm = new MyFormPanel();
    aProcessFileForm.setAction(PROCESSLISTURL);
    aProcessFileForm.setEncoding(FormPanel.ENCODING_MULTIPART);
    aProcessFileForm.setMethod(FormPanel.METHOD_POST);
    aProcessFileForm.addStyleName("fileForm");

    aFormPanel = new VerticalPanel();
    aProcessFileForm.setWidget(aFormPanel);

    Label label = new Label();
    label.setText("Upload new model");
    aFormPanel.add(label);

    aProcessUpload = new MyFileUpload();
    aProcessUpload.setName("processUpload");
    aProcessUpload.registerChangeHandler(this);
    aFormPanel.add(aProcessUpload);

    aProcessFileSubmitButton = new Button("Submit");
    aProcessFileSubmitButton.addClickHandler(this);
    aProcessUpload.addControlledWidget(aProcessFileSubmitButton);
    aFormPanel.add(aProcessFileSubmitButton);
  }

  private void startProcess() {
    final String handle = aProcessListBox.getValue(aProcessListBox.getSelectedIndex());
    TextInputPopup namePopup = new TextInputPopup("Enter name of the process instance", "Ok");
    namePopup.addInputCompleteHandler(new InputCompleteHandler() {

      @Override
      public void onComplete(InputCompleteEvent pCompleteEvent) {
        if (pCompleteEvent.isSuccess()) {
          submitStartProcess(handle, pCompleteEvent.getNewValue());
        }
      }

    });
    namePopup.show();

  }


  /**
   * @category action
   */
  private void submitStartProcess(String pHandle, String pName) {
    aStatusLabel.setText("startProcess");
    String url=PROCESSLISTURL+"/"+pHandle;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, url);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
    String postData = "op=newInstance&name="+URL.encodeComponent(pName);

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
  private void submitProcessFile() {
    aProcessFileForm.addSubmitCompleteHandler(new FileSubmitHandler());
    aProcessFileForm.submit();

    aProcessFileForm.reset();
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
  @Override
  public void onClick(ClickEvent pEvent) {
    if (pEvent.getSource()==aStartProcessButton) {
      startProcess();
    } else if (pEvent.getSource()==aRenameProcessButton) {
      renameProcess();
    } else if (pEvent.getSource()==aProcessFileSubmitButton) {
      submitProcessFile();
    } else if (pEvent.getSource()==aEditProcessButton) {
      viewProcess();
    } else if (pEvent.getSource()==aNewProcessButton) {
      newProcess();
    }
  }


  private void newProcess() {
    TextInputPopup namePopup = new TextInputPopup("Enter name of the process instance", "Ok");
    namePopup.addInputCompleteHandler(new InputCompleteHandler() {

      @Override
      public void onComplete(InputCompleteEvent pCompleteEvent) {
        if (pCompleteEvent.isSuccess()) {
          newProcess(pCompleteEvent.getNewValue());
        }
      }

    });
    namePopup.show();
  }


  protected void newProcess(String pName) {
    if (! aProcessEditPanel.isEditable()) {
      aProcessEditPanel = new ProcessEditPanel(true);
      aRoot.setRightWidget(aProcessEditPanel);
    }
    aProcessEditPanel.reset();

    aProcessListBox.setSelectedIndex(-1);
    ArrayList<ProcessNode> list = new ArrayList<ProcessNode>();
    list.add(new StartNode("start"));
    ProcessModel model = new ProcessModel(pName, list);
    aProcessEditPanel.init(model);
  }


  private void viewProcess() {
    if (aProcessEditPanel.isEditable()) {
      aProcessEditPanel = new ProcessEditPanel(false);
      aRoot.setRightWidget(aProcessEditPanel);
    }
    aProcessEditPanel.reset();
    aStatusLabel.setText("startProcess");
    String handle = aProcessListBox.getValue(aProcessListBox.getSelectedIndex());
    String URL=PROCESSLISTURL+"/"+handle;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, URL);

    try {
      rb.sendRequest(null, new RequestCallback() {

        @Override
        public void onError(Request pRequest, Throwable pException) {
          aStatusLabel.setText("Error ("+pException.getMessage()+")");
        }

        @Override
        public void onResponseReceived(Request pRequest, Response pResponse) {
          aStatusLabel.setText("Process received, loading...");
          aProcessEditPanel.init(pResponse);
        }

      });
    } catch (RequestException e) {
      aStatusLabel.setText("Error ("+e.getMessage()+")");
    }

  }


  private void renameProcess() {
    final String handle = aProcessListBox.getValue(aProcessListBox.getSelectedIndex());
    TextInputPopup renamePopup = new TextInputPopup("Enter new name of the process", "Rename");
    renamePopup.addInputCompleteHandler(new InputCompleteHandler() {

      @Override
      public void onComplete(InputCompleteEvent pCompleteEvent) {
        if (pCompleteEvent.isSuccess()) {
          aStatusLabel.setText("Rename process "+handle+" to "+pCompleteEvent.getNewValue());
          submitRenameProcess(handle, pCompleteEvent.getNewValue());
        }
      }

    });
    renamePopup.show();

  }


  private void submitRenameProcess(String pHandle, String pNewValue) {
    String url=PROCESSLISTURL+"/"+pHandle;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, url);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
    String postData;
    postData = "name="+URL.encodeComponent(pNewValue);

    try {
      rb.sendRequest(postData, new RequestCallback() {

        @Override
        public void onError(Request pRequest, Throwable pException) {
          aStatusLabel.setText("Error ("+pException.getMessage()+")");
        }

        @Override
        public void onResponseReceived(Request pRequest, Response pResponse) {
          aStatusLabel.setText("Process renamed");
          aProcessListBox.update();
        }

      });
    } catch (RequestException e) {
      GWT.log(e.getMessage(), e);
      aStatusLabel.setText("Error ("+e.getMessage()+")");
    }
  }

  /**
   * @category event handler
   */
  private void changeProcessUpload() {
    aProcessFileSubmitButton.setEnabled(aProcessUpload.getFilename().length()>0);
    aStatusLabel.setText("upload file changed");
  }

  public void setHeight(int pHeight) {
    aRoot.setHeight(pHeight);
  }

  public void start() {
    aProcessListBox.start();
  }


  public void update() {
    aProcessListBox.update();
  }


  public void stop() {
    aProcessListBox.stop();
  }


}
