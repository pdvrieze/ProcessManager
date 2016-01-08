package nl.adaptivity.process.ui.model;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.android.recyclerview.SelectableAdapter;
import nl.adaptivity.android.recyclerview.SelectableAdapter.OnSelectionListener;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.android.util.GetNameDialogFragment.Callbacks;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMEditor;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.main.SettingsActivity;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;


/**
 * A list fragment representing a list of ProcessModels. This fragment also
 * supports tablet devices by allowing list items to be given an 'activated'
 * state upon selection. This helps indicate which item is currently being
 * viewed in a {@link ProcessModelDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ProcessModelListFragment extends MasterListFragment implements LoaderCallbacks<Cursor>, Callbacks, OnRefreshListener, OnSelectionListener {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_ID = "activated_id";

  private static final int LOADERID = 3;

  private static final int REQUEST_IMPORT = 31;

  private static final String TAG = ProcessModelListFragment.class.getSimpleName();

  private static final int DLG_NEW_PM_NAME = 2;

  private PMCursorAdapter mAdapter;
  private SwipeRefreshLayout mSwipeRefresh;
  private SyncStatusObserver mSyncObserver;
  private Object mSyncObserverHandle;
  private boolean mManualSync;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ProcessModelListFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getLoaderManager().initLoader(LOADERID, null, this);
    mAdapter = new PMCursorAdapter(getActivity(), null);
    mAdapter.setOnSelectionListener(this);
    setListAdapter(mAdapter);
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

    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    return inflater.inflate(R.layout.refreshablerecyclerview, container, false);
  }

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
  }

  @Override
  public void onSelectionChanged(final SelectableAdapter adapter) {
    if (adapter.getSelectedId()!=RecyclerView.NO_ID) {
      doOnItemSelected(adapter.getSelectedPos(), adapter.getSelectedId());
    }

  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mAdapter!=null && mAdapter.getSelectedId() != RecyclerView.NO_ID) {
      // Serialize and persist the activated item position.
      outState.putLong(STATE_ACTIVATED_ID, mAdapter.getSelectedId());
    }
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
  public void onRefresh() {
    ProcessModelProvider.requestSyncProcessModelList(getActivity(), true);
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
      final boolean syncActive = ProcessModelProvider.isSyncActive(storedAccount);
      final boolean syncPending = ProcessModelProvider.isSyncPending(storedAccount);
      if (syncActive || (!syncPending)) { mManualSync= false; }
      boolean sync = syncActive || mManualSync;
      mSwipeRefresh.setRefreshing(sync);
    }
  }

  private void setActivatedId(long id) {
    ViewHolder vh = getRecyclerView().findViewHolderForItemId(id);
    if (vh!=null) {
      mAdapter.setSelectedItem(vh.getAdapterPosition());
    } else {
      mAdapter.setSelectedItem(id);
    }
  }



  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.pmlist_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_add_pm:
        createNewPM();
        return true;
      case R.id.ac_import:
        Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
        importIntent.addCategory(Intent.CATEGORY_OPENABLE);
        importIntent.setType("*/*");
        startActivityForResult(Intent.createChooser(importIntent, "Import from"),REQUEST_IMPORT);
        return true;
      case R.id.menu_settings: {
        Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(settingsIntent);
        return true;
      }
      case R.id.ac_sync_models: {
        ProcessModelProvider.requestSyncProcessModelList(getActivity(), true);
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }



  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode==Activity.RESULT_OK) {
      if (requestCode==REQUEST_IMPORT) {
        try {
          InputStream in = getActivity().getContentResolver().openInputStream(data.getData());
          try {
            DrawableProcessModel pm = PMParser.parseProcessModel(in, LayoutAlgorithm.<DrawableProcessNode>nullalgorithm(), new LayoutAlgorithm<DrawableProcessNode>());
            Uri uri = ProcessModelProvider.newProcessModel(getActivity(), pm);
            long id = ContentUris.parseId(uri);
            doOnItemSelected(AdapterView.INVALID_POSITION, id);
          } finally {
            in.close();
          }
        } catch (IOException e) {
          Log.e(TAG, "Failure to import file", e);
        }
      }
    }
  }

  private void createNewPM() {
    GetNameDialogFragment.show(getFragmentManager(), DLG_NEW_PM_NAME, "Model name", "Provide the new name", this);
  }

  @Override
  public void onNameDialogCompletePositive(GetNameDialogFragment dialog, int id, String string) {
    createNewPM(string);
  }

  @Override
  public void onNameDialogCompleteNegative(GetNameDialogFragment dialog, int id) {
    // ignore
  }

  void createNewPM(String name) {

    DrawableProcessModel model = new DrawableProcessModel(UUID.randomUUID(), name, new ArrayList<DrawableProcessNode>());
    Uri uri;
    try {
      uri = ProcessModelProvider.newProcessModel(getActivity(), model);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Intent editIntent = new Intent(getActivity(), PMEditor.class);
    editIntent.setData(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, ContentUris.parseId(uri)));
    startActivity(editIntent);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity(), ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, ProcessModels.COLUMN_NAME}, XmlBaseColumns.COLUMN_SYNCSTATE+" IS NULL OR ( " + XmlBaseColumns.COLUMN_SYNCSTATE+" != "+RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER + " AND " +XmlBaseColumns.COLUMN_SYNCSTATE+" != "+RemoteXmlSyncAdapter.SYNC_NEWDETAILSPENDING+ " )", null, null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    if (data!=null) {
      mAdapter.changeCursor(data);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.changeCursor(null);
  }
}
