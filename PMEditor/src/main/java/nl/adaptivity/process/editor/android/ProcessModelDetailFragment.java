package nl.adaptivity.process.editor.android;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.process.android.ProcessModelUtil;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.editor.android.PMProcessesFragment.PMProvider;
import nl.adaptivity.process.editor.android.databinding.FragmentProcessmodelDetailBinding;
import nl.adaptivity.process.models.ProcessModelHolder;
import nl.adaptivity.process.models.ProcessModelLoader;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;

import java.io.IOException;
import java.util.UUID;

/**
 * A fragment representing a single ProcessModel detail screen. This fragment is
 * either contained in a {@link MainActivity} in two-pane mode (on
 * tablets) or a {@link ProcessModelDetailActivity} on handsets.
 */
public class ProcessModelDetailFragment extends PMProcessesFragment implements LoaderCallbacks<ProcessModelHolder>, OnClickListener, PMProvider {

  public interface Callbacks {
    void onProcessModelSelected(long processModelId);

    void onInstantiateModel(long modelId, String suggestedName);
  }

  private class ModelViewLayoutChangeListener implements OnLayoutChangeListener {

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
      if (mItem!=null && ((oldRight-oldLeft!=right-left)||(oldBottom-oldTop!=bottom-top))) {
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

  private static final int DLG_NEW_MODEL_NAME_CLONE = 3;

  Callbacks mCallbacks;

  /**
   * The process model represented by this fragment
   */
  private BaseProcessAdapter mItem;

  private PMProcessesFragment mProcessesFragment;

  private long mProcessModelId;

  private Long mModelHandle;
  private FragmentProcessmodelDetailBinding mBinding;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public ProcessModelDetailFragment() {}

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof Callbacks) {
      mCallbacks = (Callbacks) activity;
    }
  }

  void updateDiagramScale() {
    RectF diagramBounds =new RectF();
    mItem.getBounds(diagramBounds);
    final DiagramView diagramView = mBinding.diagramView1;
    float scale = Math.min(diagramView.getWidth()/diagramBounds.width(),diagramView.getHeight()/diagramBounds.height());
    diagramView.setScale(scale);

    float w2 = diagramView.getWidth()/scale;

    diagramView.setOffsetX(diagramBounds.left-(w2-diagramBounds.width())/2);

    float h2 = diagramView.getHeight()/scale;
    diagramView.setOffsetY(diagramBounds.top-(h2-diagramBounds.height())/2);
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
    mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_processmodel_detail, container, false);
    mBinding.setData(new ProcessModelHolder());

    mBinding.diagramView1.addOnLayoutChangeListener(new ModelViewLayoutChangeListener());

    mBinding.btnPmEdit.setOnClickListener(this);

    mBinding.btnPmExec.setOnClickListener(this);

    mBinding.btnPmClone.setOnClickListener(this);

    mBinding.btnPmPublish.setOnClickListener(this);

    return mBinding.getRoot();
  }

  @Override
  public Loader<ProcessModelHolder> onCreateLoader(int id, Bundle args) {
    mProcessModelId = args.getLong(ARG_ITEM_ID);
    Uri uri = ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_STREAM_BASE,mProcessModelId);
    return new ProcessModelLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(Loader<ProcessModelHolder> loader, ProcessModelHolder data) {
    mBinding.processmodelDetailSpinner.setVisibility(View.GONE);
    mBinding.setData(data);

    if (data.model==null) {
      mBinding.diagramView1.setAdapter(null);
      mItem = null;
      mModelHandle = null;

    } else {
      mBinding.diagramView1.getParent().requestLayout(); // Do a layout
      mItem = new BaseProcessAdapter(DrawableProcessModel.get(data.model));
      mModelHandle = data.handle;
      mBinding.diagramView1.setAdapter(mItem);
      updateDiagramScale();
    }
  }

  @Override
  public void onLoaderReset(Loader<ProcessModelHolder> loader) {
    mBinding.processmodelName.setText(null);
    mItem = null;
    mBinding.diagramView1.setAdapter(null);
    // TODO Auto-generated method stub

  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
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
    long id = getArguments().getLong(ARG_ITEM_ID);
    mCallbacks.onInstantiateModel(id, mBinding.processmodelName.getText()+" Instance");
  }

  public void btnPmCloneClicked() {
    CharSequence previousName = mBinding.processmodelName.getText();
    String suggestedNewName = ProcessModelUtil.suggestNewName(getActivity(), previousName);

    GetNameDialogFragment.show(getFragmentManager(), DLG_NEW_MODEL_NAME_CLONE, "Model name", "Provide the new name", new GetNameDialogFragment.Callbacks() {

      @Override
      public void onNameDialogCompletePositive(GetNameDialogFragment dialog, int id, String string) {
        cloneWithName(string);
      }

      @Override
      public void onNameDialogCompleteNegative(GetNameDialogFragment dialog, int id) {
        // ignore
      }
    }, suggestedNewName);
    // Don't do anything yet
  }

  protected void cloneWithName(String newName) {
    // TODO Auto-generated method stub
    DrawableProcessModel currentModel = ((BaseProcessAdapter) mBinding.diagramView1.getAdapter()).getDiagram();
    DrawableProcessModel newModel = currentModel.clone();
    newModel.setName(newName);
    newModel.setUuid(UUID.randomUUID());

    Uri uri;
    try {
      uri = ProcessModelProvider.newProcessModel(getActivity(), newModel);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (mCallbacks!=null) {
      mCallbacks.onProcessModelSelected(ContentUris.parseId(uri));
    }
  }

  public void btnPmPublishClicked() {
    Uri itemUri = getCurrentProcessUri();
    ContentValues cv = new ContentValues(1);
    cv.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER));
    final ContentResolver contentResolver = getActivity().getContentResolver();
    contentResolver.update(itemUri, cv, null, null);
    mBinding.btnPmPublish.setEnabled(false);
    MainActivity.requestSyncProcessModelList(getActivity(), true);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.pm_detail_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId()==R.id.ac_delete) {
      onDeleteItem();
      return true;
    }
    return super.onOptionsItemSelected(item);
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
      mCallbacks.onProcessModelSelected(-1);
    }
    return result;
  }

  private Uri getCurrentProcessUri() {
    return ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, mProcessModelId);
  }

  @Override
  public ClientProcessModel<?, ?> getProcessModel() {
    return mItem.getDiagram();
  }
}
