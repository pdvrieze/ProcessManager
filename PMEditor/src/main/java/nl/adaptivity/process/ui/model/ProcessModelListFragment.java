package nl.adaptivity.process.ui.model;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.android.util.SelectableCursorAdapter;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.editor.android.PMEditor;
import nl.adaptivity.process.editor.android.PMParser;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.SettingsActivity;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.model.ModelListItemBinding;
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
public class ProcessModelListFragment extends MasterListFragment implements LoaderCallbacks<Cursor>, nl.adaptivity.android.util.GetNameDialogFragment.Callbacks {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_POSITION = "activated_position";

  private static final int LOADERID = 3;

  private static final int REQUEST_IMPORT = 31;

  private static final String TAG = ProcessModelListFragment.class.getSimpleName();

  private static final int DLG_NEW_PM_NAME = 2;

  final class PMCursorAdapter extends SelectableCursorAdapter<PMCursorAdapter.PMViewHolder> {

    class PMViewHolder extends SelectableCursorAdapter.SelectableViewHolder {

      final ModelListItemBinding binding;

      public PMViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
        super(inflater.inflate(R.layout.modellist_item, parent, false));
        binding = DataBindingUtil.bind(itemView);
      }
    }

    private LayoutInflater mInflater;
    private int mNameColumn;

    private PMCursorAdapter(Context context, Cursor c) {
      super(context, c);
      mInflater = LayoutInflater.from(context);
      updateColumnIndices(c);
      setHasStableIds(true);
    }

    private void updateColumnIndices(Cursor c) {
      if (c==null) {
        mNameColumn = -1;
      } else {
        mNameColumn = c.getColumnIndex(ProcessModels.COLUMN_NAME);
      }
    }

    @Override
    public void changeCursor(Cursor cursor) {
      super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
      Log.d(TAG, "Swapping processmodel cursor");
      final Cursor result = super.swapCursor(newCursor);
      updateColumnIndices(newCursor);
      return result;
    }

    @Override
    public void onBindViewHolder(final PMViewHolder viewHolder, final Cursor cursor) {
      super.onBindViewHolder(viewHolder, cursor);
      viewHolder.binding.setName(mNameColumn>=0 ? cursor.getString(mNameColumn): null);
      viewHolder.binding.executePendingBindings();
    }

    @Override
    public PMViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
      return new PMViewHolder(mInflater, parent);
    }

    @Override
    public void onClickView(final View v, final int adapterPosition) {
      super.onClickView(v, adapterPosition);
      doOnItemSelected(adapterPosition, getItemId(adapterPosition));
    }
  }

  private PMCursorAdapter mAdapter;

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
    if (mAdapter!=null && mAdapter.getSelection() != RecyclerView.NO_POSITION) {
      // Serialize and persist the activated item position.
      outState.putInt(STATE_ACTIVATED_POSITION, mAdapter.getSelection());
    }
  }

  private void setActivatedPosition(int position) {
    mAdapter.setSelection(position);
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
