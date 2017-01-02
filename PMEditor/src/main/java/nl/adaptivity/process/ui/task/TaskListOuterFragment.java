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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.task;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import nl.adaptivity.android.util.MasterDetailOuterFragment;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.android.util.MasterListFragment.ListCallbacks;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.process.editor.android.R;


/**
 * An activity representing a list of ProcessModels. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link TaskDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link TaskListFragment} and the item details (if present) is a
 * {@link TaskDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ListCallbacks} interface to listen for item
 * selections.
 */
public class TaskListOuterFragment extends MasterDetailOuterFragment {


  public interface TaskListCallbacks {

    void requestSyncTaskList(boolean immediate, long minAge);

    void onShowTask(long taskId);

    ProcessSyncManager getSyncManager();
  }



  public TaskListOuterFragment() {
    super(R.layout.outer_task_list, R.id.task_list, R.id.task_detail_container);
  }

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet
   * device.
   */
  private boolean mTwoPane;
  private TaskListCallbacks mCallbacks;

  public void showTask(final long taskId) {
    onItemSelected(taskId, true);
  }

  public static TaskListOuterFragment newInstance(final long taskId) {
    final TaskListOuterFragment result = new TaskListOuterFragment();
    if (taskId!=0) {
      result.setArguments(addArgs(null, taskId));
    }
    return result;
  }

  @Override
  protected MasterListFragment createListFragment(final Bundle args) {
    final TaskListFragment taskListFragment = new TaskListFragment();
    if (args!=null) { taskListFragment.setArguments(args); }
    return taskListFragment;
  }

  @Override
  protected TaskDetailFragment createDetailFragment(final long itemId) {
    final TaskDetailFragment fragment  = new TaskDetailFragment();
    final Bundle             arguments = new Bundle();
    arguments.putLong(ARG_ITEM_ID, itemId);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  protected Intent getDetailIntent(final long itemId) {
    final Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
    intent.putExtra(ARG_ITEM_ID, itemId);
    return intent;
  }

  @Override
  public ProcessSyncManager getSyncManager() {
    return mCallbacks.getSyncManager();
  }

  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    if (activity instanceof TaskListCallbacks) {
      mCallbacks = (TaskListCallbacks) activity;
      mCallbacks.requestSyncTaskList(true, ProcessSyncManager.DEFAULT_MIN_AGE); // request immediate sync
    }
  }

  @Override
  public CharSequence getTitle(final Context context) {
    return context.getString(R.string.title_tasklist);
  }
}
