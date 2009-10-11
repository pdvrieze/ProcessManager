package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.base.client.MyFileUpload;
import nl.adaptivity.gwt.base.client.MyFormPanel;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteEvent;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteHandler;
import nl.adaptivity.gwt.ext.client.RemoteListBox;
import nl.adaptivity.process.userMessageHandler.client.RenamePopup.RenameEvent;
import nl.adaptivity.process.userMessageHandler.client.RenamePopup.RenameHandler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.*;


public class ProcessesPanel extends Composite implements ClickHandler, ChangeHandler {

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

  SplittedFillLeftPanel<Widget> aRoot;

  private Button aEditProcessButton;

  private ProcessEditPanel aProcessEditPanel;

  public ProcessesPanel(Label pStatusLabel) {
    aRoot = new SplittedFillLeftPanel<Widget>();

//    HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();

    aStatusLabel = pStatusLabel;

    initListBox();

    initLowerPanel();

    aProcessEditPanel = new ProcessEditPanel();
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

    aStartProcessButton = createLeftButton("Start process");
    aEditProcessButton = createLeftButton("View process");
    aRenameProcessButton = createLeftButton("Rename process");

    initUploadPanel();

    aLowerPanel.add(aProcessFileForm);

    aProcessUpload.setWidth("100%");
    aProcessFileForm.setWidth("100%");
    aFormPanel.setWidth("100%");
  }


  private Button createLeftButton(String pCaption) {
    Button result = new Button(pCaption);
    aProcessListBox.addControlledWidget(result);
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
      editProcess();
    }
  }


  private void editProcess() {
    aProcessEditPanel.reset();
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
    RenamePopup renamePopup = new RenamePopup("Process");
    renamePopup.addRenameHandler(new RenameHandler() {

      @Override
      public void onRename(RenameEvent pRenameEvent) {
        aStatusLabel.setText("Rename process "+handle+" to "+pRenameEvent.getNewValue());
        submitRenameProcess(handle, pRenameEvent.getNewValue());
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
