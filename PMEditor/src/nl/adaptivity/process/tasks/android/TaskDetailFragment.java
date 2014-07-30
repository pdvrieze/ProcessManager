package nl.adaptivity.process.tasks.android;

import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.data.TaskLoader;
import nl.adaptivity.process.tasks.data.TaskProvider;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A fragment representing a single ProcessModel detail screen. This fragment is
 * either contained in a {@link TaskListOuterFragment} in two-pane mode (on
 * tablets) or a {@link TaskDetailActivity} on handsets.
 */
public class TaskDetailFragment extends Fragment implements LoaderCallbacks<UserTask>, OnClickListener {

  /**
   * The fragment argument representing the item ID that this fragment
   * represents.
   */
  public static final String ARG_ITEM_ID = "item_id";

  private static final int LOADER_TASKITEM = 0;

  private TextView mTVSummary;

  private ProgressBar mSpinner;

  private long mTaskId;

  private UserTask mUserTask;

  private LinearLayout mDetailView;

  private TextView mTVState;

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
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_task_detail, container, false);
    mSpinner = (ProgressBar) rootView.findViewById(R.id.task_detail_spinner);
    mDetailView = (LinearLayout) rootView.findViewById(R.id.task_detail);
    mSpinner.setVisibility(View.VISIBLE);
    mDetailView.setVisibility(View.GONE);


    mTVSummary = (TextView) mDetailView.findViewById(R.id.task_name);
    mTVState = (TextView) mDetailView.findViewById(R.id.task_state);

    mDetailView.findViewById(R.id.btn_task_complete).setOnClickListener(this);
    return rootView;
  }

  @Override
  public Loader<UserTask> onCreateLoader(int pId, Bundle pArgs) {
    mTaskId = pArgs.getLong(ARG_ITEM_ID);
    Uri uri = ContentUris.withAppendedId(TaskProvider.Tasks.CONTENT_ID_URI_BASE,mTaskId);
    return new TaskLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(Loader<UserTask> pLoader, UserTask pData) {
    mSpinner.setVisibility(View.GONE);
    mDetailView.setVisibility(View.VISIBLE);
    mTVSummary.setText(pData.getSummary());
    mTVState.setText(pData.getState());
    mUserTask = pData;
  }

  @Override
  public void onLoaderReset(Loader<UserTask> pLoader) {
    mTVSummary.setText(null);
    mTVState.setText(null);
    mUserTask = null;
  }

  @Override
  public void onClick(View pV) {
//    switch (pV.getId()) {
//      case R.id.btn_pm_edit:
//        btnPmEditClicked(); return;
//      case R.id.btn_pm_exec:
//        btnPmExecClicked(); return;
//    }
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
