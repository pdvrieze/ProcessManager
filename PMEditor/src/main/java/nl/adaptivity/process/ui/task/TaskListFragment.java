package nl.adaptivity.process.ui.task;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.*;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.android.util.SelectableCursorAdapter;
import nl.adaptivity.android.util.SelectableCursorAdapter.OnSelectionListener;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.editor.android.databinding.*;
import nl.adaptivity.process.ui.main.ListCursorLoaderCallbacks;


/**
 * An activity representing a list of Tasks. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TaskDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class TaskListFragment extends MasterListFragment implements OnRefreshListener, OnSelectionListener {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_ID = "activated_id";

  private static final int TASKLISTLOADERID = 3;

  private static final String TAG = TaskListFragment.class.getSimpleName();

  private TaskCursorAdapter mAdapter;
  private SwipeRefreshLayout mSwipeRefresh;
  private SyncStatusObserver mSyncObserver;
  private Object mSyncObserverHandle;
  private boolean mManualSync;
  private ListCursorLoaderCallbacks mTaskLoaderCallbacks;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public TaskListFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mAdapter = new TaskCursorAdapter(getActivity(), null);
    mAdapter.setOnSelectionListener(this);
    setListAdapter(mAdapter);
    mTaskLoaderCallbacks = new TaskLoaderCallbacks(getActivity(), mAdapter);
    getLoaderManager().initLoader(TASKLISTLOADERID, null, mTaskLoaderCallbacks);

    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    return inflater.inflate(R.layout.refreshablerecyclerview, container, false);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    getRecyclerView().setLayoutManager(new LinearLayoutManager(getActivity()));
    mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
    mSwipeRefresh.setOnRefreshListener(this);

    // Restore the previously serialized activated item position.
    if (savedInstanceState != null
        && savedInstanceState.containsKey(STATE_ACTIVATED_ID)) {
      setActivatedId(savedInstanceState.getLong(STATE_ACTIVATED_ID));
    }
    mSyncObserver = new SyncStatusObserver() {

      @Override
      public void onStatusChanged(final int which) {
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            updateSyncState();
          }
        });
      }
    };
  }

  @Override
  public void onResume() {
    super.onResume();
    mSyncObserver.onStatusChanged(0); // trigger status sync
    mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE | ContentResolver.SYNC_OBSERVER_TYPE_PENDING, mSyncObserver);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mSyncObserverHandle!=null) {
      ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
      mSyncObserverHandle=null;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mAdapter != null && mAdapter.getSelectedPos() != RecyclerView.NO_POSITION) {
      // Serialise and persist the activated item position.
      outState.putLong(STATE_ACTIVATED_ID, mAdapter.getSelectedId());
    }
  }

  @Override
  public void onSelectionChanged(final SelectableCursorAdapter<?> adapter) {
    doOnItemSelected(adapter.getSelectedPos(), adapter.getSelectedId());
  }

  private void setActivatedId(long itemId) {
    ViewHolder vh = getRecyclerView().findViewHolderForItemId(itemId);
    if (vh!=null) {
      mAdapter.setSelection(vh.getAdapterPosition());
    }
    mAdapter.setSelectedItem(itemId);
  }



  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.tasklist_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.ac_sync_tasks: {
        doManualRefresh();
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onRefresh() {
    TaskProvider.requestSyncTaskList(getActivity(), true);
    mManualSync=true;
  }

  private void doManualRefresh() {
    onRefresh();
    updateSyncState();
  }

  private void updateSyncState() {
    Account storedAccount = AuthenticatedWebClient.getStoredAccount(getActivity());
    if (storedAccount==null) {
      mSwipeRefresh.setRefreshing(false);
    } else {
      final boolean syncActive = TaskProvider.isSyncActive(storedAccount);
      final boolean syncPending = TaskProvider.isSyncPending(storedAccount);
      if (syncActive || (!syncPending)) { mManualSync= false; }
      boolean sync = syncActive || mManualSync;
      mSwipeRefresh.setRefreshing(sync);
    }
  }
}
