package nl.adaptivity.process.ui.task;

import java.util.NoSuchElementException;

import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.StringRes;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.FragmentTaskDetailBinding;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.UserTask.TaskState;
import nl.adaptivity.process.tasks.data.TaskLoader;
import nl.adaptivity.process.tasks.data.TaskProvider;
import android.app.Activity;
import android.content.ContentUris;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A fragment representing a single ProcessModel detail screen. This fragment is
 * either contained in a {@link TaskListOuterFragment} in two-pane mode (on
 * tablets) or a {@link TaskDetailActivity} on handsets.
 */
public class TaskDetailFragment extends Fragment implements LoaderCallbacks<UserTask>, OnClickListener {

  public interface TaskDetailCallbacks {
    public void dismissTaskDetails();
  }

  /**
   * The fragment argument representing the item ID that this fragment
   * represents.
   */
  public static final String ARG_ITEM_ID = "item_id";

  private static final int LOADER_TASKITEM = 0;


  private long mTaskId;

  private int mTaskItemFirstIndex;

  private int mTaskItemLastIndex = -1;

  private TaskDetailCallbacks mCallbacks;
  private FragmentTaskDetailBinding mBinding;
  private LinearLayout mTaskItemContainer;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public TaskDetailFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      getLoaderManager().initLoader(LOADER_TASKITEM, getArguments(), this);
    }
//    setHasOptionsMenu(true);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof TaskDetailCallbacks) {
      mCallbacks = (TaskDetailCallbacks) activity;
    } else {
      mCallbacks = null;
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_task_detail, container, false);
    mBinding.setLoading(true);

    mTaskItemContainer = (LinearLayout) mBinding.taskBelowItems.getParent();
    for(int i=mTaskItemContainer.getChildCount()-1; i>=0; --i) {
      if (mTaskItemContainer.getChildAt(i).getId()==R.id.task_below_items) {
        mTaskItemFirstIndex = i;
      }
    }
    mBinding.btnTaskComplete.setOnClickListener(this);
    return mBinding.getRoot();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mBinding.getTask()!=null) {
      try {
        TaskProvider.updateValuesAndState(getActivity(), mTaskId, mBinding.getTask());
      } catch (NoSuchElementException e) {
        Log.w(TaskDetailFragment.class.getSimpleName(), "The task no longer exists", e);
      } catch (RemoteException | OperationApplicationException e) {
        Log.w(TaskDetailFragment.class.getSimpleName(), "Failure to update the task state", e);
        Toast.makeText(getActivity(), "The task could not be stored", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public Loader<UserTask> onCreateLoader(int id, Bundle args) {
    mTaskId = args.getLong(ARG_ITEM_ID);
    Uri uri = ContentUris.withAppendedId(TaskProvider.Tasks.CONTENT_ID_URI_BASE, mTaskId);
    return new TaskLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(Loader<UserTask> loader, UserTask data) {
    mBinding.setLoading(false);
    if (data==null) { onLoaderReset(loader); return;}
    mBinding.setTask(data);

    int viewPos = mTaskItemFirstIndex;
    if (mTaskItemLastIndex>mTaskItemFirstIndex) {
      mTaskItemContainer.removeViews(mTaskItemFirstIndex, mTaskItemLastIndex-mTaskItemFirstIndex);
    }
    LayoutInflater inflater = LayoutInflater.from(getActivity());
    for(TaskItem item: data.getItems()) {
      View taskView = item.createView(inflater, mTaskItemContainer);

      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f);
      mTaskItemContainer.addView(taskView, viewPos, params);
      ++viewPos;
    }
    mTaskItemLastIndex = mTaskItemFirstIndex+data.getItems().size();
  }

  @Override
  public void onLoaderReset(Loader<UserTask> loader) {
    mBinding.setTask(null);
    if (mTaskItemLastIndex>mTaskItemFirstIndex) {
      mTaskItemContainer.removeViews(mTaskItemFirstIndex, mTaskItemLastIndex-mTaskItemFirstIndex);
    }
    mTaskItemLastIndex=-1;
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_task_complete:
        onCompleteTaskClicked();
    }
  }

  private void onCompleteTaskClicked() {
    mBinding.getTask().setState(TaskState.Complete);
    if (mCallbacks!=null) {
      mCallbacks.dismissTaskDetails();
    }
  }

  public UserTask getUserTask() {
    return mBinding.getTask();
  }
}
