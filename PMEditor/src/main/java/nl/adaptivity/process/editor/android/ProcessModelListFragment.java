package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

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

  /**
   * The current activated item position. Only used on tablets.
   */
  private int mActivatedPosition = AdapterView.INVALID_POSITION;

  private static final class PMCursorAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private int mNameColumn;

    private PMCursorAdapter(Context context, Cursor c) {
      super(context, c, 0);
      mInflater = LayoutInflater.from(context);
      updateNameColumn(c);
    }

    private void updateNameColumn(Cursor c) {
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
      final Cursor result = super.swapCursor(newCursor);
      updateNameColumn(newCursor);
      return result;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      return mInflater.inflate(R.layout.modellist_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
      TextView modelName = (TextView) view.findViewById(R.id.model_name);
      if (cursor!=null && mNameColumn>=0) {
        final String name = cursor.getString(mNameColumn);
        modelName.setText(name!=null ? name : "<Unnamed>");
      } else {
        modelName.setText("<Unnamed>");
      }
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
    if (mActivatedPosition != AdapterView.INVALID_POSITION) {
      // Serialize and persist the activated item position.
      outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
    }
  }

  private void setActivatedPosition(int position) {
    if (position == AdapterView.INVALID_POSITION) {
      getListView().setItemChecked(mActivatedPosition, false);
    } else {
      getListView().setItemChecked(position, true);
    }

    mActivatedPosition = position;
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
      case R.id.ac_sync: {
        MainActivity.requestSyncProcessModelList(getActivity(), true);
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
    return new CursorLoader(getActivity(), ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, ProcessModels.COLUMN_NAME}, XmlBaseColumns.COLUMN_SYNCSTATE+" IS NULL OR ( " + XmlBaseColumns.COLUMN_SYNCSTATE+" != "+RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER + " AND " +XmlBaseColumns.COLUMN_SYNCSTATE+" != "+RemoteXmlSyncAdapter.SYNC_DETAILSPENDING+ " )", null, null);
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
