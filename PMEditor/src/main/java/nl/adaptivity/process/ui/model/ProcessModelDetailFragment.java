/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.model;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import nl.adaptivity.android.util.GetNameDialogFragment;
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.process.android.ProcessModelUtil;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.editor.android.BaseProcessAdapter;
import nl.adaptivity.process.editor.android.PMEditor;
import nl.adaptivity.process.editor.android.PMProcessesFragment;
import nl.adaptivity.process.editor.android.PMProcessesFragment.PMProvider;
import nl.adaptivity.process.editor.android.R;
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
 * either contained in a {@link nl.adaptivity.process.ui.main.OverviewActivity} in two-pane mode (on
 * tablets) or a {@link ProcessModelDetailActivity} on handsets.
 */
public class ProcessModelDetailFragment extends PMProcessesFragment implements LoaderCallbacks<ProcessModelHolder>, OnClickListener, PMProvider {

  private static final String TAG = "ProcModelDetailFrag";
  public interface ProcessModelDetailFragmentCallbacks {
    void onProcessModelSelected(long processModelId);

    void onInstantiateModel(long modelId, String suggestedName);

    ProcessSyncManager getSyncManager();
  }

  private class ModelViewLayoutChangeListener implements OnLayoutChangeListener {

    @Override
    public void onLayoutChange(final View v, final int left, final int top, final int right, final int bottom, final int oldLeft, final int oldTop, final int oldRight, final int oldBottom) {
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

  ProcessModelDetailFragmentCallbacks mCallbacks;

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
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    if (activity instanceof ProcessModelDetailFragmentCallbacks) {
      mCallbacks = (ProcessModelDetailFragmentCallbacks) activity;
    }
  }

  void updateDiagramScale() {
    final RectF diagramBounds =new RectF();
    mItem.getBounds(diagramBounds);
    final DiagramView diagramView = mBinding.diagramView1;
    float scale = Math.min(diagramView.getWidth()/diagramBounds.width(),diagramView.getHeight()/diagramBounds.height());
    diagramView.setScale(scale);
    scale = (float) diagramView.getScale();

    final float w2 = diagramView.getWidth() / scale;

    diagramView.setOffsetX(diagramBounds.left-(w2-diagramBounds.width())/2);

    final float h2 = diagramView.getHeight() / scale;
    diagramView.setOffsetY(diagramBounds.top-(h2-diagramBounds.height())/2);
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (getArguments().containsKey(ARG_ITEM_ID)) {
      getLoaderManager().initLoader(LOADER_ITEM, getArguments(), this);
    }
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                           final Bundle savedInstanceState) {
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
  public void onPause() {
    super.onPause();
    if (mModelHandle!=null) {
      final boolean checked = mBinding.checkboxFavourite.isChecked();
      if (checked != mBinding.getData().isFavourite()) {
        mBinding.getData().setFavourite(mBinding.checkboxFavourite.isChecked());
        final Uri           uri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, mProcessModelId);
        final ContentValues cv  = new ContentValues(1);
        cv.put(ProcessModels.COLUMN_FAVOURITE, checked);
        getActivity().getContentResolver().update(uri, cv, null, null);
      }
    }
  }

  @Override
  public Loader<ProcessModelHolder> onCreateLoader(final int id, final Bundle args) {
    mProcessModelId = args.getLong(ARG_ITEM_ID);
    final Uri uri = ContentUris.withAppendedId(ProcessModelProvider.ProcessModels.CONTENT_ID_STREAM_BASE, mProcessModelId);
    return new ProcessModelLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(final Loader<ProcessModelHolder> loader, final ProcessModelHolder data) {
    Log.d(TAG, "onLoadFinished: ");
    mBinding.processmodelDetailSpinner.setVisibility(View.GONE);
    mBinding.setData(data);

    if (data==null || data.model==null) {
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
  public void onLoaderReset(final Loader<ProcessModelHolder> loader) {
    mBinding.processmodelName.setText(null);
    mItem = null;
    mBinding.diagramView1.setAdapter(null);
  }

  @Override
  public void onClick(final View v) {
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
    final Intent intent = new Intent(getActivity(), PMEditor.class);
    final long   id     = getArguments().getLong(ARG_ITEM_ID);
    intent.setData(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_STREAM_BASE, id));
    startActivity(intent);
  }

  public void btnPmExecClicked() {
    final long id = getArguments().getLong(ARG_ITEM_ID);
    mCallbacks.onInstantiateModel(id, mBinding.processmodelName.getText()+" Instance");
  }

  public void btnPmCloneClicked() {
    final CharSequence previousName     = mBinding.processmodelName.getText();
    final String       suggestedNewName = ProcessModelUtil.suggestNewName(getActivity(), previousName);

    GetNameDialogFragment.show(getFragmentManager(), DLG_NEW_MODEL_NAME_CLONE, "Model name", "Provide the new name", new GetNameDialogFragmentCallbacks() {

      @Override
      public void onNameDialogCompletePositive(final GetNameDialogFragment dialog, final int id, final String string) {
        cloneWithName(string);
      }

      @Override
      public void onNameDialogCompleteNegative(final GetNameDialogFragment dialog, final int id) {
        // ignore
      }
    }, suggestedNewName);
    // Don't do anything yet
  }

  protected void cloneWithName(final String newName) {
    // TODO Auto-generated method stub
    final DrawableProcessModel currentModel = ((BaseProcessAdapter) mBinding.diagramView1.getAdapter()).getDiagram();
    final DrawableProcessModel newModel     = currentModel.clone();
    newModel.setName(newName);
    newModel.setUuid(UUID.randomUUID());

    final Uri uri;
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
    final Uri           itemUri = getCurrentProcessUri();
    final ContentValues cv      = new ContentValues(1);
    cv.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER));
    final ContentResolver contentResolver = getActivity().getContentResolver();
    contentResolver.update(itemUri, cv, null, null);
    mBinding.btnPmPublish.setEnabled(false);
    // XXX verify that this is really not needed
//    mCallbacks.getSyncManager().requestSyncProcessModelList(true, minAge);
  }

  @Override
  public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    inflater.inflate(R.menu.pm_detail_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    if (item.getItemId()==R.id.ac_delete) {
      onDeleteItem();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private boolean onDeleteItem() {
    final Uri     uri = getCurrentProcessUri();
    final boolean result;
    if (mModelHandle==null) {
      result = getActivity().getContentResolver().delete(uri, null, null)>0;
    } else {
      final ContentValues cv = new ContentValues(1);
      cv.put(ProcessModels.COLUMN_SYNCSTATE, Integer.valueOf(RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER));
      result = getActivity().getContentResolver().update(uri, cv , null, null)>0;
      // XXX verify that this is really not needed
//      mCallbacks.getSyncManager().requestSyncProcessModelList(true, minAge);
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
