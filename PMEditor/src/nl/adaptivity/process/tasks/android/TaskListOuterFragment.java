package nl.adaptivity.process.tasks.android;

import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.data.TaskProvider;
import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
public class TaskListOuterFragment extends TitleFragment implements TaskListFragment.Callbacks {


  public interface TaskListCallbacks {

    void requestSyncTaskList(boolean pImmediate);

  }

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet
   * device.
   */
  private boolean mTwoPane;
  private TaskListCallbacks mCallbacks;

  @Override
  public View onCreateView(LayoutInflater pInflater, ViewGroup pContainer, Bundle pSavedInstanceState) {
    View view = pInflater.inflate(R.layout.activity_task_list, pContainer, false);
    if (view.findViewById(R.id.task_detail_container) != null) {
      // The detail container view will be present only in the
      // large-screen layouts (res/values-large and
      // res/values-sw600dp). If this view is present, then the
      // activity should be in two-pane mode.
      mTwoPane = true;

      // In two-pane mode, list items should be given the
      // 'activated' state when touched.
      ((TaskListFragment) getChildFragmentManager()
          .findFragmentById(R.id.task_list))
          .setActivateOnItemClick(true);
    }
    return view;
  }

  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    if (pActivity instanceof TaskListCallbacks) {
      mCallbacks = (TaskListCallbacks) pActivity;
      mCallbacks.requestSyncTaskList(true); // request immediate sync
    }
  }



  /**
   * Callback method from {@link TaskListFragment.Callbacks} indicating
   * that the item with the given ID was selected.
   */
  @Override
  public void onItemSelected(long pProcessModelRowId) {
    Bundle arguments = new Bundle();
    arguments.putLong(TaskDetailFragment.ARG_ITEM_ID, pProcessModelRowId);
    TaskDetailFragment fragment = new TaskDetailFragment();
    fragment.setArguments(arguments);
    if (mTwoPane) {
      // In two-pane mode, show the detail in the detail pane
      getChildFragmentManager().beginTransaction()
          .replace(R.id.processmodel_detail_container, fragment)
          .commit();

    } else {
      // In single pane mode replace the main content
      getFragmentManager().beginTransaction()
          .replace(R.id.fragment_main_content, fragment)
          .commit();
    }
  }

  public static void requestSync(Account account) {
    Bundle extras = new Bundle(1);
    extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    ContentResolver.requestSync(account, TaskProvider.AUTHORITY, extras );
  }

  @Override
  public CharSequence getTitle(Context pContext) {
    return pContext.getString(R.string.title_tasklist);
  }
}
