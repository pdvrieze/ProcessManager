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

package nl.adaptivity.process.ui.task;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import nl.adaptivity.android.util.MasterDetailOuterFragment;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.android.util.MasterListFragment.ProcessModelListCallbacks;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.sync.SyncManager;


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
 * {@link ProcessModelListCallbacks} interface to listen for item
 * selections.
 */
public class TaskListOuterFragment extends MasterDetailOuterFragment {


  public interface TaskListCallbacks {

    void requestSyncTaskList(boolean immediate);

    void onShowTask(long taskId);

    SyncManager getSyncManager();
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
    TaskListOuterFragment result = new TaskListOuterFragment();
    if (taskId!=0) {
      result.setArguments(addArgs(null, taskId));
    }
    return result;
  }

  @Override
  protected MasterListFragment createListFragment(Bundle args) {
    TaskListFragment taskListFragment = new TaskListFragment();
    if (args!=null) { taskListFragment.setArguments(args); }
    return taskListFragment;
  }

  @Override
  protected TaskDetailFragment createDetailFragment(long itemId) {
    TaskDetailFragment fragment = new TaskDetailFragment();
    Bundle arguments = new Bundle();
    arguments.putLong(ARG_ITEM_ID, itemId);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  protected Intent getDetailIntent(long itemId) {
    Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
    intent.putExtra(ARG_ITEM_ID, itemId);
    return intent;
  }

  @Override
  public SyncManager getSyncManager() {
    return mCallbacks.getSyncManager();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof TaskListCallbacks) {
      mCallbacks = (TaskListCallbacks) activity;
      mCallbacks.requestSyncTaskList(true); // request immediate sync
    }
  }

  @Override
  public CharSequence getTitle(Context context) {
    return context.getString(R.string.title_tasklist);
  }
}
