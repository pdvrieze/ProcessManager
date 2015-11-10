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


public class ProcessesPanel extends ResizeComposite implements ClickHandler, ChangeHandler {

  private final class FileSubmitHandler implements SubmitCompleteHandler {

    @Override
    public void onSubmitComplete(final SubmitCompleteEvent event) {
      mStatusLabel.setText("File submit complete!!");
      final com.google.gwt.dom.client.Document results = event.getResults();
      if (results != null) {
        mProcessListBox.update(results);
      } else {
        mProcessListBox.update();
      }
    }
  }

  private static final String PROCESSLISTURL = PEUserMessageHandler.BASEURL + "/ProcessEngine/processModels";

  private Button mStartProcessButton;

  private RemoteListBox mProcessListBox;

  private MyFormPanel mProcessFileForm;

  private Button mProcessFileSubmitButton;

  private MyFileUpload mProcessUpload;

  private final Label mStatusLabel;

  private VerticalPanel mFormPanel;

  private Button mRenameProcessButton;

  private VerticalPanel mLowerPanel;

  private final SplittedFillLeftPanel<Widget> mRoot;

  private Button mEditProcessButton;

  private ProcessEditPanel mProcessEditPanel;

  private Button mNewProcessButton;

  private Button mDeleteProcessButton;

  public ProcessesPanel(final Label statusLabel) {
    mRoot = new SplittedFillLeftPanel<Widget>();
    mRoot.setTopLeftWidget(new HTML("Top Left"));
    mRoot.setRightWidget(new HTML("Right"));

    //    HorizontalSplitPanel mainPanel = new HorizontalSplitPanel();

    mStatusLabel = statusLabel;

    initListBox();

    initLowerPanel();

    mProcessEditPanel = new ProcessEditPanel(false);
    mProcessEditPanel.setInstance(false);
    mRoot.setRightWidget(mProcessEditPanel);

    initWidget(mRoot);
  }


  private void initListBox() {
    mProcessListBox = new RemoteListBox(PROCESSLISTURL);
    mProcessListBox.setRootElement("processModels");
    mProcessListBox.setListElement("processModel");
    mProcessListBox.setValueElement("@handle");
    mProcessListBox.setTextElement("=@{handle}: @{name}");
    mRoot.setTopLeftWidget(mProcessListBox);

    mProcessListBox.addStyleName("mhList");
    mProcessListBox.addStyleName("tabContent");
  }


  private void initLowerPanel() {
    mLowerPanel = new VerticalPanel();
    mLowerPanel.addStyleName("tabContent");
    mRoot.setBottomLeftWidget(mLowerPanel);

    mStartProcessButton = createLeftButton("Start process", true);
    mNewProcessButton = createLeftButton("New process", false);
    mEditProcessButton = createLeftButton("View process", true);
    mRenameProcessButton = createLeftButton("Rename process", true);
    mDeleteProcessButton = createLeftButton("Delete process", true);

    initUploadPanel();

    mLowerPanel.add(mProcessFileForm);

    mProcessUpload.setWidth("100%");
    mProcessFileForm.setWidth("100%");
    mFormPanel.setWidth("100%");
  }


  private Button createLeftButton(final String caption, final boolean controlled) {
    final Button result = new Button(caption);
    if (controlled) {
      mProcessListBox.addControlledWidget(result);
    }
    result.addStyleName("inTabButton");
    result.addClickHandler(this);
    mLowerPanel.add(result);
    return result;
  }


  private void initUploadPanel() {
    mProcessFileForm = new MyFormPanel();
    mProcessFileForm.setAction(PROCESSLISTURL);
    mProcessFileForm.setEncoding(FormPanel.ENCODING_MULTIPART);
    mProcessFileForm.setMethod(FormPanel.METHOD_POST);
    mProcessFileForm.addStyleName("fileForm");

    mFormPanel = new VerticalPanel();
    mProcessFileForm.setWidget(mFormPanel);

    final Label label = new Label();
    label.setText("Upload new model");
    mFormPanel.add(label);

    mProcessUpload = new MyFileUpload();
    mProcessUpload.setName("processUpload");
    mProcessUpload.registerChangeHandler(this);
    mFormPanel.add(mProcessUpload);

    mProcessFileSubmitButton = new Button("Submit");
    mProcessFileSubmitButton.addClickHandler(this);
    mProcessUpload.addControlledWidget(mProcessFileSubmitButton);
    mFormPanel.add(mProcessFileSubmitButton);
  }

  private void startProcess() {
    final String handle = mProcessListBox.getValue(mProcessListBox.getSelectedIndex());
    final TextInputPopup namePopup = new TextInputPopup("Enter name of the process instance", "Ok");
    namePopup.addInputCompleteHandler(new InputCompleteHandler() {

      @Override
      public void onComplete(final InputCompleteEvent completeEvent) {
        if (completeEvent.isSuccess()) {
          submitStartProcess(handle, completeEvent.getNewValue());
        }
      }

    });
    namePopup.show();

  }


  /**
   * @category action
   */
  private void submitStartProcess(final String handle, final String name) {
    mStatusLabel.setText("startProcess");
    final String url = PROCESSLISTURL + "/" + handle;
    final RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, url);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
    final String postData = "op=newInstance&name=" + URL.encodeComponent(name);

    try {
      rb.sendRequest(postData, new RequestCallback() {

        @Override
        public void onError(final Request request, final Throwable exception) {
          mStatusLabel.setText("Error (" + exception.getMessage() + ")");
        }

        @Override
        public void onResponseReceived(final Request request, final Response response) {
          mStatusLabel.setText("Process instantiated");
          // TODO perhaps do something with this, but the instances are not visible from the tab
          //          mInstanceListBox.update();
        }

      });
    } catch (final RequestException e) {
      mStatusLabel.setText("Error (" + e.getMessage() + ")");
    }

  }

  /**
   * @category action
   */
  private void submitProcessFile() {
    mProcessFileForm.addSubmitCompleteHandler(new FileSubmitHandler());
    mProcessFileForm.submit();

    mProcessFileForm.reset();
  }

  /**
   * @category event handler
   */
  @Override
  public void onChange(final ChangeEvent event) {
    if (event.getSource() == mProcessUpload) {
      changeProcessUpload();
    }
  }

  /**
   * @category event handler
   */
  @Override
  public void onClick(final ClickEvent event) {
    if (event.getSource() == mStartProcessButton) {
      startProcess();
    } else if (event.getSource() == mRenameProcessButton) {
      renameProcess();
    } else if (event.getSource() == mProcessFileSubmitButton) {
      submitProcessFile();
    } else if (event.getSource() == mEditProcessButton) {
      viewProcess();
    } else if (event.getSource() == mDeleteProcessButton) {
      deleteProcess();
    } else if (event.getSource() == mNewProcessButton) {
      newProcess();
    }
  }


  private void newProcess() {
    final TextInputPopup namePopup = new TextInputPopup("Enter name of the process instance", "Ok");
    namePopup.addInputCompleteHandler(new InputCompleteHandler() {

      @Override
      public void onComplete(final InputCompleteEvent completeEvent) {
        if (completeEvent.isSuccess()) {
          newProcess(completeEvent.getNewValue());
        }
      }

    });
    namePopup.show();
  }


  protected void newProcess(final String name) {
    if (!mProcessEditPanel.isEditable()) {
      mProcessEditPanel = new ProcessEditPanel(true);
      mRoot.setRightWidget(mProcessEditPanel);
    }
    mProcessEditPanel.reset();

    mProcessListBox.setSelectedIndex(-1);
    final ArrayList<ProcessNode> list = new ArrayList<ProcessNode>();
    list.add(new StartNode("start"));
    final ProcessModel model = new ProcessModel(name, list);
    mProcessEditPanel.init(model);
  }


  private void viewProcess() {
    if (mProcessEditPanel.isEditable()) {
      mProcessEditPanel = new ProcessEditPanel(false);
      mRoot.setRightWidget(mProcessEditPanel);
    }
    mProcessEditPanel.reset();
    mStatusLabel.setText("startProcess");
    final String handle = mProcessListBox.getValue(mProcessListBox.getSelectedIndex());
    final String URL = PROCESSLISTURL + "/" + handle;
    final RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, URL);

    try {
      rb.sendRequest(null, new RequestCallback() {

        @Override
        public void onError(final Request request, final Throwable exception) {
          mStatusLabel.setText("Error (" + exception.getMessage() + ")");
        }

        @Override
        public void onResponseReceived(final Request request, final Response response) {
          mStatusLabel.setText("Process received, loading...");
          mProcessEditPanel.init(response);
        }

      });
    } catch (final RequestException e) {
      mStatusLabel.setText("Error (" + e.getMessage() + ")");
    }

  }

  private void deleteProcess() {
    final String handle = mProcessListBox.getValue(mProcessListBox.getSelectedIndex());
    final String url = PROCESSLISTURL + "/" + handle;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.DELETE, url);
    try {
      rb.sendRequest(null, new RequestCallback() {

        @Override
        public void onError(final Request request, final Throwable exception) {
          mStatusLabel.setText("Error (" + exception.getMessage() + ")");
          GWT.log("Error deleting process", exception);
        }

        @Override
        public void onResponseReceived(final Request request, final Response response) {
          mStatusLabel.setText("Process deleted");
          mProcessListBox.update();
        }

      });
    } catch (final RequestException e) {
      GWT.log(e.getMessage(), e);
      mStatusLabel.setText("Error (" + e.getMessage() + ")");
    }
  }

  private void renameProcess() {
    final String handle = mProcessListBox.getValue(mProcessListBox.getSelectedIndex());
    final TextInputPopup renamePopup = new TextInputPopup("Enter new name of the process", "Rename");
    renamePopup.addInputCompleteHandler(new InputCompleteHandler() {

      @Override
      public void onComplete(final InputCompleteEvent completeEvent) {
        if (completeEvent.isSuccess()) {
          mStatusLabel.setText("Rename process " + handle + " to " + completeEvent.getNewValue());
          submitRenameProcess(handle, completeEvent.getNewValue());
        }
      }

    });
    renamePopup.show();

  }

  private void submitRenameProcess(final String handle, final String newValue) {
    final String url = PROCESSLISTURL + "/" + handle;
    final RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, url);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
    String postData;
    postData = "name=" + URL.encodeComponent(newValue);

    try {
      rb.sendRequest(postData, new RequestCallback() {

        @Override
        public void onError(final Request request, final Throwable exception) {
          mStatusLabel.setText("Error (" + exception.getMessage() + ")");
        }

        @Override
        public void onResponseReceived(final Request request, final Response response) {
          mStatusLabel.setText("Process renamed");
          mProcessListBox.update();
        }

      });
    } catch (final RequestException e) {
      GWT.log(e.getMessage(), e);
      mStatusLabel.setText("Error (" + e.getMessage() + ")");
    }
  }

  /**
   * @category event handler
   */
  private void changeProcessUpload() {
    mProcessFileSubmitButton.setEnabled(mProcessUpload.getFilename().length() > 0);
    mStatusLabel.setText("upload file changed");
  }

  public void setHeight(final int height) {
    mRoot.setHeight(height);
  }

  public void start() {
    mProcessListBox.start();
  }


  public void update() {
    mProcessListBox.update();
  }


  public void stop() {
    mProcessListBox.stop();
  }


}
