package nl.adaptivity.process.ui.task;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.android.util.SelectableCursorAdapter;
import nl.adaptivity.process.editor.android.MainActivity;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.ui.task.TaskListItemBinding;


/**
 * An activity representing a list of Taskks. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TaskDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
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

  public final class TaskCursorAdapter extends nl.adaptivity.android.util.SelectableCursorAdapter<TaskListFragment.TaskCursorAdapter.TaskCursorViewHolder> {

    public final class TaskCursorViewHolder extends SelectableCursorAdapter.SelectableViewHolder {

      public final TaskListItemBinding binding;

      public TaskCursorViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
        super(inflater.inflate(R.layout.tasklist_item, parent, false));
        binding =  DataBindingUtil.bind(itemView);
      }
    }

    private LayoutInflater mInflater;
    private int mSummaryColIdx;

    private TaskCursorAdapter(Context context, Cursor c) {
      super(context, c);
      mInflater = LayoutInflater.from(context);
      updateColIdxs(c);
    }

    @Override
    public void onBindViewHolder(final TaskCursorViewHolder viewHolder, final Cursor cursor) {
      super.onBindViewHolder(viewHolder, cursor);
      viewHolder.binding.setSummary(mSummaryColIdx>=0 ? cursor.getString(mSummaryColIdx): null);
      viewHolder.binding.executePendingBindings();
    }

    @Override
    public TaskCursorViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
      return new TaskCursorViewHolder(mInflater, parent);
    }

    private void updateColIdxs(Cursor c) {
      if (c==null) {
        mSummaryColIdx = -1;
      } else {
        mSummaryColIdx = c.getColumnIndex(Tasks.COLUMN_SUMMARY);
      }
    }

    @Override
    public void changeCursor(Cursor cursor) {
      super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
      final Cursor result = super.swapCursor(newCursor);
      updateColIdxs(newCursor);
      return result;
    }

    @Override
    public void onClickView(final View v, final int adapterPosition) {
      super.onClickView(v, adapterPosition); // keep the selection event recorded
      doOnItemSelected(adapterPosition, getItemId(adapterPosition));
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
    if (savedInstanceState!=null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
      mAdapter.setSelection(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
    }
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
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mAdapter != null && mAdapter.getSelection() != RecyclerView.NO_POSITION) {
      // Serialise and persist the activated item position.
      outState.putInt(STATE_ACTIVATED_POSITION, mAdapter.getSelection());
    }
  }

  private void setActivatedPosition(int position) {
    mAdapter.setSelection(position);
  }



  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.tasklist_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.ac_sync: {
        MainActivity.requestSyncTaskList(getActivity(), true);
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity(), TaskProvider.Tasks.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, Tasks.COLUMN_SUMMARY}, Tasks.COLUMN_STATE+"!='Complete'", null, null);
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
