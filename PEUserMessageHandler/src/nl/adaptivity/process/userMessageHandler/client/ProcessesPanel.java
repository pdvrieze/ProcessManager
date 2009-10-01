package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.base.client.MyFileUpload;
import nl.adaptivity.gwt.base.client.MyFormPanel;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteEvent;
import nl.adaptivity.gwt.base.client.MyFormPanel.SubmitCompleteHandler;
import nl.adaptivity.gwt.ext.client.RemoteListBox;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Window;
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

  private VerticalPanel aButtonsPanel;

  private Button aRenameProcessButton;

  private VerticalPanel aNonListPanel;

  public ProcessesPanel(Label pStatusLabel) {
    HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();
    aStatusLabel = pStatusLabel;
    VerticalPanel leftPanel = new VerticalPanel();
    leftPanel.addStyleName("tabPanel");
    mainPanel.setLeftWidget(leftPanel);

    aProcessListBox = new RemoteListBox(PROCESSLISTURL);
    aProcessListBox.setRootElement("processModels");
    aProcessListBox.setListElement("processModel");
    aProcessListBox.setValueElement("@handle");
    aProcessListBox.setTextElement("=@{handle}: @{name}");
    leftPanel.add(aProcessListBox);
    leftPanel.setCellHeight(aProcessListBox, "100%");

    aProcessListBox.addItem("Process1");
    aProcessListBox.addItem("Process2");
    aProcessListBox.addStyleName("mhList");
    aProcessListBox.addStyleName("tabContent");

    aNonListPanel = new VerticalPanel();
    aNonListPanel.addStyleName("tabContent");
    leftPanel.add(aNonListPanel);

    aStartProcessButton = new Button("Start process");
    aProcessListBox.addControlledWidget(aStartProcessButton);
    aStartProcessButton.addStyleName("inTabButton");
    aStartProcessButton.addClickHandler(this);
    aNonListPanel.add(aStartProcessButton);

    aRenameProcessButton = new Button("Rename process");
    aProcessListBox.addControlledWidget(aRenameProcessButton);
    aRenameProcessButton.addStyleName("inTabButton");
    aRenameProcessButton.addClickHandler(this);
    aNonListPanel.add(aRenameProcessButton);

    aProcessFileForm = new MyFormPanel();
    aProcessFileForm.setAction(PROCESSLISTURL);
    aProcessFileForm.setEncoding(FormPanel.ENCODING_MULTIPART);
    aProcessFileForm.setMethod(FormPanel.METHOD_POST);
    aProcessFileForm.addStyleName("fileForm");

    aButtonsPanel = new VerticalPanel();
    aProcessFileForm.setWidget(aButtonsPanel);

    Label label = new Label();
    label.setText("Upload new model");
    aButtonsPanel.add(label);

    aProcessUpload = new MyFileUpload();
    aProcessUpload.setName("processUpload");
    aProcessUpload.registerChangeHandler(this);
    aButtonsPanel.add(aProcessUpload);


    aProcessFileSubmitButton = new Button("Submit");
    aProcessFileSubmitButton.addClickHandler(this);
    aProcessUpload.addControlledWidget(aProcessFileSubmitButton);
    aButtonsPanel.add(aProcessFileSubmitButton);

    aNonListPanel.add(aProcessFileForm);

    Label rightPanel = new Label("right");
    mainPanel.setRightWidget(rightPanel);

    int pos = (2*Window.getClientWidth())/9;
    pos = Math.max(pos, 100);
    mainPanel.setSplitPosition(pos+"px");
    aProcessListBox.setWidth("100%");
    aNonListPanel.setWidth("100%");
    aProcessUpload.setWidth("100%");
    aProcessFileForm.setWidth("100%");
    aButtonsPanel.setWidth("100%");


    initWidget(mainPanel);
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
    } else if (pEvent.getSource()==aProcessFileSubmitButton) {
      submitProcessFile();
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
    int height = pHeight;
    height -= aNonListPanel.getOffsetHeight();

    height -= 5;
    final int adjust = pHeight- height;
    final int listBoxHeight = Math.max(150, height);
    aProcessListBox.setHeight(listBoxHeight+"px");
    super.setHeight((listBoxHeight+adjust)+"px");

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
