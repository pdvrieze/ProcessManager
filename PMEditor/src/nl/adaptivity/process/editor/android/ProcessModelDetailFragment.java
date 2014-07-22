package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.util.UUID;

import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.process.android.ProcessModelUtil;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.editor.android.PMProcessesFragment.PMProvider;
import nl.adaptivity.process.models.ProcessModelLoader;
import nl.adaptivity.process.models.ProcessModelLoader.ProcessModelHolder;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A fragment representing a single ProcessModel detail screen. This fragment is
 * either contained in a {@link ProcessModelListActivity} in two-pane mode (on
 * tablets) or a {@link ProcessModelDetailActivity} on handsets.
 */
public class ProcessModelDetailFragment extends PMProcessesFragment implements LoaderCallbacks<ProcessModelHolder>, OnClickListener, PMProvider {

  public interface Callbacks {
    void onItemSelected(long pProcessModelRowId);
  }

  private class ModelViewLayoutChangeListener implements OnLayoutChangeListener {

    @Override
    public void onLayoutChange(View pV, int pLeft, int pTop, int pRight, int pBottom, int pOldLeft, int pOldTop, int pOldRight, int pOldBottom) {
      if (mItem!=null && ((pOldRight-pOldLeft!=pRight-pLeft)||(pOldBottom-pOldTop!=pBottom-pTop))) {
        updateDiagramScale();
      }
    }

  }

  /**
   * The fragment argument representing the item ID that this fragment
   * represents.
   */
  public static final String ARG_ITEM_ID = "item_id";

  private static final int LOADER_ITEM = 0;

  Callbacks mCallbacks;

  /**
   * The process model represented by this fragment
   */
  private BaseProcessAdapter mItem;

  private TextView mTVName;

  private DiagramView mModelView;

  private ProgressBar mSpinner;

  private PMProcessesFragment mProcessesFragment;

  private long mProcessModelId;

  private View mBtnPublish;

  private View mBtnExec;

  private Long mModelHandle;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ProcessModelDetailFragment() {}

  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
    if (pActivity instanceof Callbacks) {
      mCallbacks = (Callbacks) pActivity;
    }
  }

  void updateDiagramScale() {
    RectF diagramBounds =new RectF();
    mItem.getBounds(diagramBounds);
    float scale = Math.min(mModelView.getWidth()/diagramBounds.width(),mModelView.getHeight()/diagramBounds.height());
    mModelView.setScale(scale);

    float w2 = mModelView.getWidth()/scale;

    mModelView.setOffsetX(diagramBounds.left-(w2-diagramBounds.width())/2);

    float h2 = mModelView.getHeight()/scale;
    mModelView.setOffsetY(diagramBounds.top-(h2-diagramBounds.height())/2);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      getLoaderManager().initLoader(LOADER_ITEM, getArguments(), this);
    }
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_processmodel_detail, container, false);

    mTVName = (TextView) rootView.findViewById(R.id.processmodel_name);
    mModelView = (DiagramView) rootView.findViewById(R.id.diagramView1);
    mModelView.addOnLayoutChangeListener(new ModelViewLayoutChangeListener());

    mSpinner = (ProgressBar) rootView.findViewById(R.id.spinner);
    mTVName.setVisibility(View.GONE);
    mModelView.setVisibility(View.GONE);

    rootView.findViewById(R.id.btn_pm_edit).setOnClickListener(this);

    mBtnExec = rootView.findViewById(R.id.btn_pm_exec);
    mBtnExec.setOnClickListener(this);
    mBtnExec.setVisibility(View.GONE);

    rootView.findViewById(R.id.btn_pm_clone).setOnClickListener(this);

    mBtnPublish = rootView.findViewById(R.id.btn_pm_publish);
    mBtnPublish.setOnClickListener(this);
    mBtnPublish.setVisibility(View.GONE);

    return rootView;
  }

  @Override
  public Loader<ProcessModelHolder> onCreateLoader(int pId, Bundle pArgs) {
    mProcessModelId = pArgs.getLong(ARG_ITEM_ID);
    Uri uri = ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_STREAM_BASE,mProcessModelId);
    return new ProcessModelLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(Loader<ProcessModelHolder> pLoader, ProcessModelHolder pData) {
    mSpinner.setVisibility(View.GONE);
    mTVName.setVisibility(View.VISIBLE);
    mModelView.setVisibility(View.VISIBLE);
    mModelView.getParent().requestLayout(); // Do a layout
    mTVName.setText(pData.model.getName());
    mItem = new BaseProcessAdapter(DrawableProcessModel.get(pData.model));
    mModelHandle = pData.handle;
    if (pData.handle!=null) {
      mBtnPublish.setVisibility(View.GONE);
      mBtnExec.setVisibility(View.VISIBLE);
    } else {
      mBtnPublish.setVisibility(View.VISIBLE);
      mBtnExec.setVisibility(View.GONE);
    }
    mModelView.setAdapter(mItem);
    updateDiagramScale();
  }

  @Override
  public void onLoaderReset(Loader<ProcessModelHolder> pLoader) {
    mTVName.setText(null);
    mItem = null;
    mModelView.setAdapter(null);
    // TODO Auto-generated method stub

  }

  @Override
  public void onClick(View pV) {
    switch (pV.getId()) {
      case R.id.btn_pm_edit:
        btnPmEditClicked(); return;
      case R.id.btn_pm_clone:
        btnPmCloneClicked(); return;
      case R.id.btn_pm_publish:
        btnPmPublishClicked(); return;
      case R.id.btn_pm_exec:
        btnPmExecClicked(); return;
    }
  }

  public void btnPmEditClicked() {
    Intent intent = new Intent(getActivity(), PMEditor.class);
    long id = getArguments().getLong(ARG_ITEM_ID);
    intent.setData(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id));
    startActivity(intent);
  }

  public void btnPmExecClicked() {
    // Don't do anything yet
  }

  public void btnPmCloneClicked() {
    CharSequence previousName = mTVName.getText();
    String suggestedNewName = ProcessModelUtil.suggestNewName(getActivity(), previousName);

    GetNameDialogFragment.show(getFragmentManager(), "Model name", "Provide the new name", new GetNameDialogFragment.Callbacks() {

      @Override
      public void onNameDialogCompletePositive(GetNameDialogFragment pDialog, String pString) {
        cloneWithName(pString);
      }

      @Override
      public void onNameDialogCompleteNegative(GetNameDialogFragment pDialog) {
        // ignore
      }
    }, suggestedNewName);
    // Don't do anything yet
  }

  protected void cloneWithName(String pNewName) {
    // TODO Auto-generated method stub
    DrawableProcessModel currentModel = ((BaseProcessAdapter) mModelView.getAdapter()).getDiagram();
    DrawableProcessModel newModel = currentModel.clone();
    newModel.setName(pNewName);
    newModel.setUuid(UUID.randomUUID());

    Uri uri;
    try {
      uri = ProcessModelProvider.newProcessModel(getActivity(), newModel);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (mCallbacks!=null) {
      mCallbacks.onItemSelected(ContentUris.parseId(uri));
    }
  }

  public void btnPmPublishClicked() {
    Uri itemUri = getCurrentProcessUri();
    ContentValues cv = new ContentValues(1);
    cv.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER));
    final ContentResolver contentResolver = getActivity().getContentResolver();
    contentResolver.update(itemUri, cv, null, null);
    mBtnPublish.setEnabled(false);
    MainActivity.requestSyncProcessModelList(getActivity(), true);
  }

  @Override
  public void onCreateOptionsMenu(Menu pMenu, MenuInflater pInflater) {
    pInflater.inflate(R.menu.pm_detail_menu, pMenu);
    super.onCreateOptionsMenu(pMenu, pInflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem pItem) {
    if (pItem.getItemId()==R.id.ac_delete) {
      onDeleteItem();
      return true;
    }
    return super.onOptionsItemSelected(pItem);
  }

  private boolean onDeleteItem() {
    Uri uri = getCurrentProcessUri();
    boolean result;
    if (mModelHandle==null) {
      result = getActivity().getContentResolver().delete(uri, null, null)>0;
    } else {
      ContentValues cv = new ContentValues(1);
      cv.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER));
      result = getActivity().getContentResolver().update(uri, cv , null, null)>0;
      MainActivity.requestSyncProcessModelList(getActivity(), true);
    }
    if (result && mCallbacks!=null) {
      mCallbacks.onItemSelected(-1);
    }
    return result;
  }

  private Uri getCurrentProcessUri() {
    return ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, mProcessModelId);
  }

  @Override
  public ClientProcessModel<?> getProcessModel() {
    return mItem.getDiagram();
  }
}
