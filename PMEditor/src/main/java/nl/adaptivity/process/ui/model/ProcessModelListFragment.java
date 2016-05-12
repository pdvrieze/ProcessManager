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

package nl.adaptivity.process.ui.model;

import android.app.Activity;
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
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import nl.adaptivity.android.recyclerview.ClickableAdapter;
import nl.adaptivity.android.recyclerview.ClickableAdapter.OnItemClickListener;
import nl.adaptivity.android.recyclerview.SelectableAdapter;
import nl.adaptivity.android.recyclerview.SelectableAdapter.OnSelectionListener;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMEditor;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.main.SettingsActivity;
import nl.adaptivity.process.ui.model.PMCursorAdapter.PMViewHolder;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import nl.adaptivity.sync.SyncManager.SyncStatusObserverData;

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
 * Activities containing this fragment MUST implement the {@link ListCallbacks}
 * interface.
 */
public class ProcessModelListFragment extends MasterListFragment<ProcessSyncManager> implements LoaderCallbacks<Cursor>, GetNameDialogFragmentCallbacks, OnRefreshListener, OnSelectionListener, OnItemClickListener<PMViewHolder> {

  private static final int LOADERID = 3;

  private static final int REQUEST_IMPORT = 31;

  private static final String TAG = ProcessModelListFragment.class.getSimpleName();

  private static final int DLG_NEW_PM_NAME = 2;

  private SwipeRefreshLayout mSwipeRefresh;
  private SyncStatusObserver mSyncObserver;
  private SyncStatusObserverData mSyncObserverHandle;
  private boolean mManualSync;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ProcessModelListFragment() {}

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(LOADERID, null, this);
    final PMCursorAdapter adapter = new PMCursorAdapter(getActivity(), null);
    adapter.setOnSelectionListener(this);
    adapter.setOnItemClickListener(this);
    setListAdapter(adapter);

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
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    return inflater.inflate(R.layout.refreshablerecyclerview, container, false);
  }

  @Override
  public void onViewCreated(final View view, final Bundle savedInstanceState) {
    getRecyclerView().setLayoutManager(new LinearLayoutManager(getActivity()));
    mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
    mSwipeRefresh.setOnRefreshListener(this);
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onSelectionChanged(final SelectableAdapter adapter) {
    if (adapter.getSelectedId()!=RecyclerView.NO_ID) {
      doOnItemSelected(adapter.getSelectedPos(), adapter.getSelectedId());
    }

  }

  @Override
  public boolean onClickItem(final ClickableAdapter<? extends PMViewHolder> adapter, final PMViewHolder viewHolder) {
    return doOnItemClicked(viewHolder.getAdapterPosition(), viewHolder.getItemId());
  }

  @Override
  public PMCursorAdapter getListAdapter() {
    return (PMCursorAdapter) super.getListAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();
    mSyncObserverHandle = getCallbacks().getSyncManager().addOnStatusChangeObserver(ProcessModelProvider.AUTHORITY, mSyncObserver);
    mSyncObserver.onStatusChanged(0); // trigger status sync
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mSyncObserverHandle!=null) {
      getCallbacks().getSyncManager().removeOnStatusChangeObserver(mSyncObserverHandle);
      mSyncObserverHandle=null;
    }
  }

  /**
   * Called by the refresh view when a manual refresh is requested.
   */
  @Override
  public void onRefresh() {
    doManualRefresh();
  }

  private void doManualRefresh() {
    getCallbacks().getSyncManager().requestSyncProcessModelList(true);
    mManualSync=true;
    updateSyncState();
  }

  private void updateSyncState() {
    final ProcessSyncManager syncManager = getCallbacks().getSyncManager();
    if (! syncManager.isSyncable(ProcessModelProvider.AUTHORITY)) {
      mSwipeRefresh.setRefreshing(false);
    } else {
      final boolean syncActive = syncManager.isProcessModelSyncActive();
      final boolean syncPending = syncManager.isProcessModelSyncPending();
      if (syncActive || (!syncPending)) { mManualSync= false; }
      final boolean sync = syncActive || mManualSync;
      mSwipeRefresh.setRefreshing(sync);
    }
  }



  @Override
  public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    inflater.inflate(R.menu.pmlist_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_add_pm:
        createNewPM();
        return true;
      case R.id.ac_import:
        final Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
        importIntent.addCategory(Intent.CATEGORY_OPENABLE);
        importIntent.setType("*/*");
        startActivityForResult(Intent.createChooser(importIntent, "Import from"),REQUEST_IMPORT);
        return true;
      case R.id.menu_settings: {
        final Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(settingsIntent);
        return true;
      }
      case R.id.ac_sync_models: {
        doManualRefresh();
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }



  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (resultCode==Activity.RESULT_OK) {
      if (requestCode==REQUEST_IMPORT) {
        try {
          final InputStream in = getActivity().getContentResolver().openInputStream(data.getData());
          try {
            final DrawableProcessModel pm  = PMParser.parseProcessModel(in, LayoutAlgorithm.<DrawableProcessNode>nullalgorithm(), new LayoutAlgorithm<DrawableProcessNode>());
            pm.setHandle(-1);
            final Uri                  uri = ProcessModelProvider.newProcessModel(getActivity(), pm);
            final long                 id  = ContentUris.parseId(uri);
            if (isVisible()&& ! isRemoving()) {
              doOnItemSelected(AdapterView.INVALID_POSITION, id);
            }
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
  public void onNameDialogCompletePositive(final GetNameDialogFragment dialog, final int id, final String string) {
    createNewPM(string);
  }

  @Override
  public void onNameDialogCompleteNegative(final GetNameDialogFragment dialog, final int id) {
    // ignore
  }

  void createNewPM(final String name) {

    final DrawableProcessModel model = new DrawableProcessModel(UUID.randomUUID(), name, new ArrayList<DrawableProcessNode>());
    final Uri                  uri;
    try {
      uri = ProcessModelProvider.newProcessModel(getActivity(), model);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final Intent editIntent = new Intent(getActivity(), PMEditor.class);
    editIntent.setData(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, ContentUris.parseId(uri)));
    startActivity(editIntent);
  }

  @Override
  public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
    return new CursorLoader(getActivity(), ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, ProcessModels.COLUMN_NAME}, XmlBaseColumns.COLUMN_SYNCSTATE+" IS NULL OR ( " + XmlBaseColumns.COLUMN_SYNCSTATE+" != "+RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER + " AND " +XmlBaseColumns.COLUMN_SYNCSTATE+" != "+RemoteXmlSyncAdapter.SYNC_NEWDETAILSPENDING+ " )", null, null);
  }

  @Override
  public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
    if (data!=null) {
      getListAdapter().changeCursor(data);
    }
  }

  @Override
  public void onLoaderReset(final Loader<Cursor> loader) {
    getListAdapter().changeCursor(null);
  }
}
