package nl.adaptivity.process.tasks.android;

import java.util.NoSuchElementException;

import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
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

  private static final String TASK_COMPLETTE = "Complete";

  private TextView mTVSummary;

  private ProgressBar mSpinner;

  private long mTaskId;

  private UserTask mUserTask;

  private LinearLayout mDetailView;

  private TextView mTVState;

  private LinearLayout mTaskItemContainer;

  private int mTaskItemFirstIndex;

  private int mTaskItemLastIndex = -1;

  private TaskDetailCallbacks mCallbacks;

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
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    if (pActivity instanceof TaskDetailCallbacks) {
      mCallbacks = (TaskDetailCallbacks) pActivity;
    } else {
      mCallbacks = null;
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_task_detail, container, false);
    mSpinner = (ProgressBar) rootView.findViewById(R.id.task_detail_spinner);
    mDetailView = (LinearLayout) rootView.findViewById(R.id.task_detail);
    mSpinner.setVisibility(View.VISIBLE);
    mDetailView.setVisibility(View.GONE);


    mTVSummary = (TextView) mDetailView.findViewById(R.id.task_name);
    mTVState = (TextView) mDetailView.findViewById(R.id.task_state);

    mTaskItemContainer = (LinearLayout) mDetailView.findViewById(R.id.task_below_items).getParent();
    for(int i=mTaskItemContainer.getChildCount()-1; i>=0; --i) {
      if (mTaskItemContainer.getChildAt(i).getId()==R.id.task_below_items) {
        mTaskItemFirstIndex = i;
      }
    }

    mDetailView.findViewById(R.id.btn_task_complete).setOnClickListener(this);
    return rootView;
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mUserTask!=null) {
      try {
        TaskProvider.updateValuesAndState(getActivity(), mTaskId, mUserTask);
      } catch (NoSuchElementException e) {
        Log.w(TaskDetailFragment.class.getSimpleName(), "The task no longer exists", e);
      } catch (RemoteException | OperationApplicationException e) {
        Log.w(TaskDetailFragment.class.getSimpleName(), "Failure to update the task state", e);
        Toast.makeText(getActivity(), "The task could not be stored", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public Loader<UserTask> onCreateLoader(int pId, Bundle pArgs) {
    mTaskId = pArgs.getLong(ARG_ITEM_ID);
    Uri uri = ContentUris.withAppendedId(TaskProvider.Tasks.CONTENT_ID_URI_BASE, mTaskId);
    return new TaskLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(Loader<UserTask> pLoader, UserTask pData) {
    if (pData==null) { onLoaderReset(pLoader); return;}
    mSpinner.setVisibility(View.GONE);
    mDetailView.setVisibility(View.VISIBLE);
    mTVSummary.setText(pData.getSummary());
    mTVState.setText(pData.getState());
    int viewPos = mTaskItemFirstIndex;
    if (mTaskItemLastIndex>mTaskItemFirstIndex) {
      mTaskItemContainer.removeViews(mTaskItemFirstIndex, mTaskItemLastIndex-mTaskItemFirstIndex);
    }
    LayoutInflater inflater = LayoutInflater.from(getActivity());
    for(TaskItem item: pData.getItems()) {
      View taskView = item.createView(inflater, mTaskItemContainer);

      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f);
      mTaskItemContainer.addView(taskView, viewPos, params);
      ++viewPos;
    }
    mTaskItemLastIndex = mTaskItemFirstIndex+pData.getItems().size();
    mUserTask = pData;
  }

  @Override
  public void onLoaderReset(Loader<UserTask> pLoader) {
    mTVSummary.setText(null);
    mTVState.setText(null);
    if (mTaskItemLastIndex>mTaskItemFirstIndex) {
      mTaskItemContainer.removeViews(mTaskItemFirstIndex, mTaskItemLastIndex-mTaskItemFirstIndex);
    }
    mTaskItemLastIndex=-1;
    mUserTask = null;

  }

  @Override
  public void onClick(View pV) {
    switch (pV.getId()) {
      case R.id.btn_task_complete:
        onCompleteTaskClicked();
    }
  }

  private void onCompleteTaskClicked() {
    mUserTask.setState(TASK_COMPLETTE);
    if (mCallbacks!=null) {
      mCallbacks.dismissTaskDetails();
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
//    pInflater.inflate(R.menu.pm_detail_menu, pMenu);
    super.onCreateOptionsMenu(pMenu, pInflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem pItem) {
    return super.onOptionsItemSelected(pItem);
  }

  public UserTask getUserTask() {
    return mUserTask;
  }
}
