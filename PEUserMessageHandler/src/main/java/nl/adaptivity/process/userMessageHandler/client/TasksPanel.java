/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

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

  private final Button mStartTaskButton;

  private final Button mTakeTaskButton;

  private final Button mCompleteTaskButton;

  private final Label mStatusLabel;

  private final RemoteListBox mTaskListBox;

  public TasksPanel(final Label statusLabel) {
    mStatusLabel = statusLabel;
    final SplittedFillLeftPanel<RemoteListBox> root = new SplittedFillLeftPanel<RemoteListBox>();
    //    root.addStyleName("tabPanel");

    mTaskListBox = new RemoteListBox(TASKLISTURL);
    mTaskListBox.addStyleName("mhList");
    mTaskListBox.addStyleName("tabContent");
    mTaskListBox.setRootElement("tasks");
    mTaskListBox.setTextElement("=@summary (@{state})");
    mTaskListBox.setValueElement("@handle");
    mTaskListBox.setListElement("task");

    root.setTopLeftWidget(mTaskListBox);
    //    mTaskListBox.addChangeHandler(this);

    final VerticalPanel vp1 = new VerticalPanel();
    root.setBottomLeftWidget(vp1);
    vp1.addStyleName("tabContent");


    mTakeTaskButton = new Button("Take task");
    mTaskListBox.addControlledWidget(mTakeTaskButton);
    mTakeTaskButton.addStyleName("inTabButton");
    vp1.add(mTakeTaskButton);
    mTakeTaskButton.addClickHandler(this);

    mStartTaskButton = new Button("Start task");
    mTaskListBox.addControlledWidget(mStartTaskButton);
    mStartTaskButton.addStyleName("inTabButton");
    vp1.add(mStartTaskButton);
    mStartTaskButton.addClickHandler(this);

    mCompleteTaskButton = new Button("Complete task");
    mTaskListBox.addControlledWidget(mCompleteTaskButton);
    mCompleteTaskButton.addStyleName("inTabButton");
    vp1.add(mCompleteTaskButton);
    mCompleteTaskButton.addClickHandler(this);

    initWidget(root);
  }

  /**
   * @category event handler
   */
  @Override
  public void onClick(final ClickEvent event) {
    if (event.getSource() == mStartTaskButton) {
      startTask();
    } else if (event.getSource() == mTakeTaskButton) {
      takeTask();
    } else if (event.getSource() == mCompleteTaskButton) {
      completeTask();
    }
  }

  /**
   * @category action
   */
  private void startTask() {
    mStatusLabel.setText("startTask");
    final String newState = "Started";
    updateTaskState(newState, mTaskListBox.getValue(mTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void takeTask() {
    mStatusLabel.setText("takeTask");
    final String newState = "Taken";
    updateTaskState(newState, mTaskListBox.getValue(mTaskListBox.getSelectedIndex()));
  }

  /**
   * @category action
   */
  private void completeTask() {
    mStatusLabel.setText("completeTask");
    final String newState = "Finished";
    updateTaskState(newState, mTaskListBox.getValue(mTaskListBox.getSelectedIndex()));
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
          mStatusLabel.setText("Error (" + exception.getMessage() + ")");
        }

        @Override
        public void onResponseReceived(final Request request, final Response response) {
          mTaskListBox.update();
        }

      });
    } catch (final RequestException e) {
      mStatusLabel.setText("Error (" + e.getMessage() + ")");
    }
  }


  public void start() {
    mTaskListBox.start();
  }


  public void update() {
    mTaskListBox.update();
  }


  public void stop() {
    mTaskListBox.stop();
  }


}
