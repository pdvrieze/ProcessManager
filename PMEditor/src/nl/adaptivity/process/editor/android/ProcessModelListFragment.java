package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.util.ArrayList;

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
public class ProcessModelListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_POSITION = "activated_position";

  private static final int LOADERID = 3;

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

  public static class GetPMNameDialogFragment extends DialogFragment {

    private static final String ARG_PREV_NAME = "prevName";
    ProcessModelListFragment mOwner;

    public GetPMNameDialogFragment() { /* empty */ }

    @Override
    public void onAttach(Activity activity) {
      super.onAttach(activity);
      mOwner = (ProcessModelListFragment) activity.getFragmentManager().findFragmentById(R.id.processmodel_list);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      String prevName = getArguments()==null ? null : getArguments().getString(ARG_PREV_NAME);
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      final EditText editText = new EditText(getActivity());
      editText.setInputType(InputType.TYPE_CLASS_TEXT);
      editText.setText(prevName);
      editText.selectAll();
      builder.setTitle("Model name")
             .setMessage("Provide the new name")
             .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

              @Override
              public void onClick(DialogInterface pDialog, int id) {
                mOwner.createNewPM(editText.getText().toString());
              }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .setView(editText);
      return builder.create();

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
    }
    return super.onOptionsItemSelected(pItem);
  }

  private void createNewPM() {
    GetPMNameDialogFragment f = new GetPMNameDialogFragment();
//    Bundle args = new Bundle(1);
//    args.putString(ARG_PREV_NAME, null);
//    f.setArguments(args);
    f.show(getFragmentManager(), "getName");
  }

  void createNewPM(String name) {

    DrawableProcessModel model = new DrawableProcessModel(name, new ArrayList<DrawableProcessNode>());
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
