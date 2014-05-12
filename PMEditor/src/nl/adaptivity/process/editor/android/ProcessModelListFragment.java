package nl.adaptivity.process.editor.android;

import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.processModel.ProcessModel;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
  private int mActivatedPosition = ListView.INVALID_POSITION;

  private static final class PMCursorAcapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private int mNameColumn;

    private PMCursorAcapter(Context pContext, Cursor pC) {
      super(pContext, pC, 0);
      mInflater = LayoutInflater.from(pContext);
      if (pC==null) {
        mNameColumn = -1;
      } else {
        mNameColumn = pC.getColumnIndex(ProcessModels.COLUMN_NAME);
      }
    }

    @Override
    public View newView(Context pContext, Cursor pCursor, ViewGroup pParent) {
      return mInflater.inflate(android.R.layout.simple_list_item_1, pParent, false);
    }

    @Override
    public void bindView(View pView, Context pContext, Cursor pCursor) {
      TextView view = (TextView) pView;
      if (pCursor!=null) {
        view.setText(pCursor.getString(mNameColumn));
      } else {
        view.setText(null);
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
    public void onItemSelected(ProcessModel<?> pProcessModel);
  }

  /**
   * A dummy implementation of the {@link Callbacks} interface that does
   * nothing. Used only when this fragment is not attached to an activity.
   */
  private static Callbacks sDummyCallbacks = new Callbacks() {

    @Override
    public void onItemSelected(ProcessModel<?> pProcessModel) {}
  };

  private PMCursorAcapter mAdapter;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ProcessModelListFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getLoaderManager().initLoader(LOADERID, null, this);
    mAdapter = new PMCursorAcapter(getActivity(), null);
    setListAdapter(mAdapter);

    // TODO: replace with a real list adapter.
//    setListAdapter(new CursorAdapter(getActivity(), ArrayAdapter<DummyContent.DummyItem>(
//        getActivity(),
//        android.R.layout.simple_list_item_activated_1,
//        android.R.id.text1,
//        DummyContent.ITEMS));
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

    // Notify the active callbacks interface (the activity, if the
    // fragment is attached to one) that an item has been selected.
    mCallbacks.onItemSelected((ProcessModel<?>)listView.getAdapter().getItem(position));
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
