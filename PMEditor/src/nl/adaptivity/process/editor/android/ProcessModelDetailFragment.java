package nl.adaptivity.process.editor.android;

import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.models.ProcessModelLoader;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.processModel.ProcessModel;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.Loader;
import android.graphics.RectF;
import android.net.Uri;
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
  private DrawableProcessModel mItem;

  private TextView mTVName;

  private DiagramView mModelView;

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

    mTVName = (TextView) rootView.findViewById(R.id.processmodel_name);
    mModelView = (DiagramView) rootView.findViewById(R.id.diagramView1);

    // Show the dummy content as text in a TextView.
    if (mItem != null) {
      ((TextView) rootView.findViewById(R.id.processmodel_detail)).setText(mItem.getName());
    }

    return rootView;
  }

  @Override
  public Loader<ProcessModel<?>> onCreateLoader(int pId, Bundle pArgs) {
    Uri uri = ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_STREAM_BASE,pArgs.getLong(ARG_ITEM_ID));
    return new ProcessModelLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(Loader<ProcessModel<?>> pLoader, ProcessModel<?> pData) {
    mItem = DrawableProcessModel.get(pData);
    mTVName.setText(pData.getName());
    final BaseProcessAdapter adapter = new BaseProcessAdapter(mItem);
    RectF diagramBounds =new RectF();
    adapter.getBounds(diagramBounds);
    float scale = Math.min(mModelView.getWidth()/diagramBounds.width(),mModelView.getHeight()/diagramBounds.height());
    mModelView.setAdapter(adapter);
    mModelView.setScale(scale);
    mModelView.setOffsetX(diagramBounds.left);
  }

  @Override
  public void onLoaderReset(Loader<ProcessModel<?>> pLoader) {
    mTVName.setText(null);
    mItem = null;
    mModelView.setAdapter(null);
    // TODO Auto-generated method stub

  }
}
