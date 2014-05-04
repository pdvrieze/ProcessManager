package nl.adaptivity.process.editor.android;

import nl.adaptivity.process.processModel.ProcessModel;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A fragment representing a single ProcessModel detail screen. This fragment is
 * either contained in a {@link ProcessModelListActivity} in two-pane mode (on
 * tablets) or a {@link ProcessModelDetailActivity} on handsets.
 */
public class ProcessModelDetailFragment extends Fragment implements LoaderCallbacks<ProcessModel<?>> {

  /**
   * The fragment argument representing the item ID that this fragment
   * represents.
   */
  public static final String ARG_ITEM_ID = "item_id";

  private static final int LOADER_ITEM = 0;

  /**
   * The process model represented by this fragment
   */
  private ProcessModel<?> mItem;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ProcessModelDetailFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      getLoaderManager().initLoader(LOADER_ITEM, getArguments(), this);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_processmodel_detail, container, false);

    // Show the dummy content as text in a TextView.
    if (mItem != null) {
      ((TextView) rootView.findViewById(R.id.processmodel_detail)).setText(mItem.getName());
    }

    return rootView;
  }

  @Override
  public Loader<ProcessModel<?>> onCreateLoader(int pId, Bundle pArgs) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onLoadFinished(Loader<ProcessModel<?>> pLoader, ProcessModel<?> pData) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onLoaderReset(Loader<ProcessModel<?>> pLoader) {
    // TODO Auto-generated method stub

  }
}
