package nl.adaptivity.process.tasks.android;

import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.process.editor.android.MainActivity;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A list fragment representing a list of ProcessModels. This fragment also
 * supports tablet devices by allowing list items to be given an 'activated'
 * state upon selection. This helps indicate which item is currently being
 * viewed in a {@link TaskDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class TaskListFragment extends MasterListFragment implements LoaderCallbacks<Cursor> {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_POSITION = "activated_position";

  private static final int TASKLISTLOADERID = 3;

  private static final String TAG = TaskListFragment.class.getSimpleName();

  /**
   * The current activated item position. Only used on tablets.
   */
  private int mActivatedPosition = AdapterView.INVALID_POSITION;

  private static final class TaskCursorAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private int mSummaryColIdx;

    private TaskCursorAdapter(Context pContext, Cursor pC) {
      super(pContext, pC, 0);
      mInflater = LayoutInflater.from(pContext);
      updateColIdxs(pC);
    }

    private void updateColIdxs(Cursor pC) {
      if (pC==null) {
        mSummaryColIdx = -1;
      } else {
        mSummaryColIdx = pC.getColumnIndex(Tasks.COLUMN_SUMMARY);
      }
    }

    @Override
    public void changeCursor(Cursor pCursor) {
      super.changeCursor(pCursor);
    }

    @Override
    public Cursor swapCursor(Cursor pNewCursor) {
      final Cursor result = super.swapCursor(pNewCursor);
      updateColIdxs(pNewCursor);
      return result;
    }

    @Override
    public View newView(Context pContext, Cursor pCursor, ViewGroup pParent) {
      return mInflater.inflate(R.layout.tasklist_item, pParent, false);
    }

    @Override
    public void bindView(View pView, Context pContext, Cursor pCursor) {
      TextView tvSummary = (TextView) pView.findViewById(R.id.model_name);
      if (pCursor!=null && mSummaryColIdx>=0) {
        final String summary = pCursor.getString(mSummaryColIdx);
        tvSummary.setText(summary!=null ? summary : "<Unnamed>");
      } else {
        tvSummary.setText("<Unnamed>");
      }
    }
  }

  private TaskCursorAdapter mAdapter;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public TaskListFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getLoaderManager().initLoader(TASKLISTLOADERID, null, this);
    mAdapter = new TaskCursorAdapter(getActivity(), null);
    setListAdapter(mAdapter);

    setHasOptionsMenu(true);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Restore the previously serialized activated item position.
    if (savedInstanceState != null
        && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
      setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
    }
  }

  @Override
  public void onListItemClick(ListView listView, View view, int position, long id) {
    super.onListItemClick(listView, view, position, id);

    long modelid = mAdapter.getItemId(position);
    // Notify the active callbacks interface (the activity, if the
    // fragment is attached to one) that an item has been selected.
    doOnItemSelected(position, modelid);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mActivatedPosition != ListView.INVALID_POSITION) {
      // Serialise and persist the activated item position.
      outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
    }
  }

  private void setActivatedPosition(int position) {
    if (position == ListView.INVALID_POSITION) {
      getListView().setItemChecked(mActivatedPosition, false);
    } else {
      getListView().setItemChecked(position, true);
    }

    mActivatedPosition = position;
  }



  @Override
  public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
    pInflater.inflate(R.menu.tasklist_menu, pMenu);
    super.onCreateOptionsMenu(pMenu, pInflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem pItem) {
    switch (pItem.getItemId()) {
      case R.id.ac_sync: {
        MainActivity.requestSyncTaskList(getActivity(), true);
        return true;
      }
    }
    return super.onOptionsItemSelected(pItem);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int pId, Bundle pArgs) {
    return new CursorLoader(getActivity(), TaskProvider.Tasks.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, Tasks.COLUMN_SUMMARY}, null, null, null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> pLoader, Cursor pData) {

    if (pData!=null) {
      mAdapter.changeCursor(pData);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> pLoader) {
    mAdapter.changeCursor(null);
  }
}
