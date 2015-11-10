package nl.adaptivity.process.tasks.android;

import nl.adaptivity.android.util.MasterDetailOuterFragment;
import nl.adaptivity.android.util.MasterListFragment;
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

    void requestSyncTaskList(boolean immediate);

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
  protected TaskDetailFragment createDetailFragment(int row, long itemId) {
    TaskDetailFragment fragment = new TaskDetailFragment();
    Bundle arguments = new Bundle();
    arguments.putLong(TaskDetailFragment.ARG_ITEM_ID, itemId);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  protected Intent getDetailIntent(int row, long itemId) {
    Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
    intent.putExtra(TaskDetailFragment.ARG_ITEM_ID, itemId);
    return intent;
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
