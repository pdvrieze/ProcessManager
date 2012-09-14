package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.ext.client.RemoteListBox;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;


public class TasksPanel extends Composite implements ClickHandler {

  private static final String TASKLISTURL = PEUserMessageHandler.BASEURL+"/PEUserMessageService/UserMessageService/pendingTasks";

  private Button aStartTaskButton;
  private Button aTakeTaskButton;
  private Button aCompleteTaskButton;

  private final Label aStatusLabel;

  private RemoteListBox aTaskListBox;

  public TasksPanel(Label pStatusLabel) {
    aStatusLabel = pStatusLabel;
    SplittedFillLeftPanel<RemoteListBox> root = new SplittedFillLeftPanel<RemoteListBox>();
//    root.addStyleName("tabPanel");

    aTaskListBox = new RemoteListBox(TASKLISTURL);
    aTaskListBox.addStyleName("mhList");
    aTaskListBox.addStyleName("tabContent");
    aTaskListBox.setRootElement("tasks");
    aTaskListBox.setTextElement("=@summary (@{state})");
    aTaskListBox.setValueElement("@handle");
    aTaskListBox.setListElement("task");

    root.setTopLeftWidget(aTaskListBox);
//    aTaskListBox.addChangeHandler(this);

    VerticalPanel vp1 = new VerticalPanel();
    root.setBottomLeftWidget(vp1);
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

    initWidget(root);
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
    }
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


  public void start() {
    aTaskListBox.start();
  }


  public void update() {
    aTaskListBox.update();
  }


  public void stop() {
    aTaskListBox.stop();
  }


}
