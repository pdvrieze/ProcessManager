package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
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
public class ProcessModelListFragment extends ListFragment implements LoaderCallbacks<Cursor>, nl.adaptivity.android.util.GetNameDialogFragment.Callbacks {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_POSITION = "activated_position";

  private static final int LOADERID = 3;

  private static final int REQUEST_IMPORT = 31;

  private static final String TAG = ProcessModelListFragment.class.getSimpleName();

  /**
   * The fragment's current callback object, which is notified of list item
   * clicks.
   */
  private Callbacks mCallbacks = sDummyCallbacks;

  /**
   * The current activated item position. Only used on tablets.
   */
  private int mActivatedPosition = AdapterView.INVALID_POSITION;

  private static final class PMCursorAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private int mNameColumn;

    private PMCursorAdapter(Context pContext, Cursor pC) {
      super(pContext, pC, 0);
      mInflater = LayoutInflater.from(pContext);
      updateNameColumn(pC);
    }

    private void updateNameColumn(Cursor pC) {
      if (pC==null) {
        mNameColumn = -1;
      } else {
        mNameColumn = pC.getColumnIndex(ProcessModels.COLUMN_NAME);
      }
    }

    @Override
    public void changeCursor(Cursor pCursor) {
      super.changeCursor(pCursor);
    }

    @Override
    public Cursor swapCursor(Cursor pNewCursor) {
      final Cursor result = super.swapCursor(pNewCursor);
      updateNameColumn(pNewCursor);
      return result;
    }

    @Override
    public View newView(Context pContext, Cursor pCursor, ViewGroup pParent) {
      return mInflater.inflate(R.layout.modellist_item, pParent, false);
    }

    @Override
    public void bindView(View pView, Context pContext, Cursor pCursor) {
      TextView modelName = (TextView) pView.findViewById(R.id.model_name);
      if (pCursor!=null && mNameColumn>=0) {
        final String name = pCursor.getString(mNameColumn);
        modelName.setText(name!=null ? name : "<Unnamed>");
      } else {
        modelName.setText("<Unnamed>");
      }
    }
  }

  /**
   * A callback interface that all activities containing this fragment must
   * implement. This mechanism allows activities to be notified of item
   * selections.
   */
  public interface Callbacks {

    /**
     * Callback for when an item has been selected.
     */
    public void onItemSelected(long pProcessModelRowId);
  }

  /**
   * A dummy implementation of the {@link Callbacks} interface that does
   * nothing. Used only when this fragment is not attached to an activity.
   */
  private static Callbacks sDummyCallbacks = new Callbacks() {

    @Override
    public void onItemSelected(long pProcessModelRowId) {/*dummy*/}
  };

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
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Activities containing this fragment must implement its callbacks.
    if (!(activity instanceof Callbacks)) {
      throw new IllegalStateException("Activity must implement fragment's callbacks.");
    }

    mCallbacks = (Callbacks) activity;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    // Reset the active callbacks interface to the dummy implementation.
    mCallbacks = sDummyCallbacks;
  }

  @Override
  public void onListItemClick(ListView listView, View view, int position, long id) {
    super.onListItemClick(listView, view, position, id);

    long modelid = mAdapter.getItemId(position);
    // Notify the active callbacks interface (the activity, if the
    // fragment is attached to one) that an item has been selected.
    mCallbacks.onItemSelected(modelid);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mActivatedPosition != ListView.INVALID_POSITION) {
      // Serialize and persist the activated item position.
      outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
    }
  }

  /**
   * Turns on activate-on-click mode. When this mode is on, list items will be
   * given the 'activated' state when touched.
   */
  public void setActivateOnItemClick(boolean activateOnItemClick) {
    // When setting CHOICE_MODE_SINGLE, ListView will automatically
    // give items the 'activated' state when touched.
    getListView().setChoiceMode(activateOnItemClick
        ? ListView.CHOICE_MODE_SINGLE
        : ListView.CHOICE_MODE_NONE);
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
    pInflater.inflate(R.menu.pmlist_menu, pMenu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem pItem) {
    switch (pItem.getItemId()) {
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
    }
    return super.onOptionsItemSelected(pItem);
  }



  @Override
  public void onActivityResult(int pRequestCode, int pResultCode, Intent pData) {
    if (pResultCode==Activity.RESULT_OK) {
      if (pRequestCode==REQUEST_IMPORT) {
        try {
          InputStream in = getActivity().getContentResolver().openInputStream(pData.getData());
          try {
            DrawableProcessModel pm = PMParser.parseProcessModel(in, null /*LayoutAlgorithm.<DrawableProcessNode>nullalgorithm()*/);
            Uri uri = ProcessModelProvider.newProcessModel(getActivity(), pm);
            long id = ContentUris.parseId(uri);
            mCallbacks.onItemSelected(id);
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
    GetNameDialogFragment.show(getFragmentManager(), "Model name", "Provide the new name", this);
  }

  @Override
  public void onNameDialogCompletePositive(GetNameDialogFragment pDialog, String pString) {
    createNewPM(pString);
  }

  @Override
  public void onNameDialogCompleteNegative(GetNameDialogFragment pDialog) {
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
  public Loader<Cursor> onCreateLoader(int pId, Bundle pArgs) {
    return new CursorLoader(getActivity(), ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, ProcessModels.COLUMN_NAME}, null, null, null);
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
