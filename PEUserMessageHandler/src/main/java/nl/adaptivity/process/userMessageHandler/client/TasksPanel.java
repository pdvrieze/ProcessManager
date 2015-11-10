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

  private static final String TASKLISTURL = PEUserMessageHandler.BASEURL + "/PEUserMessageHandler/UserMessageService/pendingTasks";

  private final Button aStartTaskButton;

  private final Button aTakeTaskButton;

  private final Button aCompleteTaskButton;

  private final Label aStatusLabel;

  private final RemoteListBox aTaskListBox;

  public TasksPanel(final Label statusLabel) {
    aStatusLabel = statusLabel;
    final SplittedFillLeftPanel<RemoteListBox> root = new SplittedFillLeftPanel<RemoteListBox>();
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

    final VerticalPanel vp1 = new VerticalPanel();
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
  public void onClick(final ClickEvent event) {
    if (event.getSource() == aStartTaskButton) {
      startTask();
    } else if (event.getSource() == aTakeTaskButton) {
      takeTask();
    } else if (event.getSource() == aCompleteTaskButton) {
      completeTask();
    }
  }

  /**
   * @category action
   */
  private void startTask() {
    aStatusLabel.setText("startTask");
    final String newState = "Started";
    updateTaskState(newState, aTaskListBox.getValue(aTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void takeTask() {
    aStatusLabel.setText("takeTask");
    final String newState = "Taken";
    updateTaskState(newState, aTaskListBox.getValue(aTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void completeTask() {
    aStatusLabel.setText("completeTask");
    final String newState = "Finished";
    updateTaskState(newState, aTaskListBox.getValue(aTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void updateTaskState(final String newState, final String handle) {
    final String URL = TASKLISTURL + "/" + handle;
    final RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, URL);
    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
    final String postData = "state=" + newState;

    try {
      rb.sendRequest(postData, new RequestCallback() {

        @Override
        public void onError(final Request request, final Throwable exception) {
          aStatusLabel.setText("Error (" + exception.getMessage() + ")");
        }

        @Override
        public void onResponseReceived(final Request request, final Response response) {
          aTaskListBox.update();
        }

      });
    } catch (final RequestException e) {
      aStatusLabel.setText("Error (" + e.getMessage() + ")");
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
