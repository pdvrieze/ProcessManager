package nl.adaptivity.process.tasks.android;

import nl.adaptivity.android.util.MasterDetailOuterFragment;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.process.editor.android.ProcessModelDetailFragment;
import nl.adaptivity.process.editor.android.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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
 * {@link TaskListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class TaskListOuterFragment extends MasterDetailOuterFragment {


  public interface TaskListCallbacks {

    void requestSyncTaskList(boolean pImmediate);

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

  @Override
  protected MasterListFragment createListFragment() {
    return new TaskListFragment();
  }

  @Override
  protected ProcessModelDetailFragment createDetailFragment(int pRow, long pItemId) {
    ProcessModelDetailFragment fragment = new ProcessModelDetailFragment();
    Bundle arguments = new Bundle();
    arguments.putLong(TaskDetailFragment.ARG_ITEM_ID, pItemId);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  protected Intent getDetailIntent(int pRow, long pItemId) {
    Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
    intent.putExtra(TaskDetailFragment.ARG_ITEM_ID, pItemId);
    return intent;
  }

  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    if (pActivity instanceof TaskListCallbacks) {
      mCallbacks = (TaskListCallbacks) pActivity;
      mCallbacks.requestSyncTaskList(true); // request immediate sync
    }
  }

  @Override
  public CharSequence getTitle(Context pContext) {
    return pContext.getString(R.string.title_tasklist);
  }
}
