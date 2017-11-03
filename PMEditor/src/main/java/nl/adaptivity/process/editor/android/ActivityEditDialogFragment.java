/*
 * Copyright (c) 2017.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.editor.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import nl.adaptivity.android.graphics.RadioButtonHelper;
import nl.adaptivity.android.graphics.RadioButtonHelper.OnCheckedChangeListener;
import nl.adaptivity.process.diagram.DrawableActivity;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.DrawableProcessNode.Builder;
import nl.adaptivity.process.diagram.android.ParcelableActivity;
import nl.adaptivity.process.editor.android.databinding.DlgNodeEditActivityBinding;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.process.ui.activity.UserTaskEditorActivity;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.VariableReference;
import nl.adaptivity.process.util.VariableReference.ResultReference;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;


/**
 * A dialogfragment for editing activity nodes.
 */
public class ActivityEditDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, OnEditorActionListener, OnCheckedChangeListener, View.OnClickListener {

  private static final int MAX_MAX = 20;

  private int mPos=-1;

  private DlgNodeEditActivityBinding mBinding;
  private DrawableActivity mActivityNode;

  @Override
  public void onCheckedChanged(final RadioButtonHelper source, final int oldCheckedId, final int newCheckedId) {
    mBinding.editAcHuman.setEnabled(newCheckedId==R.id.radio_ac_human);
    mBinding.editAcService.setEnabled(newCheckedId==R.id.radio_ac_service);
  }

  /**
   * Create a new dialog
   * @param position The number of the activity in the containing model
   * @return The dialog fragment
   */
  public static ActivityEditDialogFragment newInstance(final int position) {
    final ActivityEditDialogFragment frag = new ActivityEditDialogFragment();
    final Bundle args = new Bundle(1);
    args.putInt(UIConstants.KEY_NODE_POS, position);
    frag.setArguments(args);
    return frag;
  }

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    mPos = getArguments().getInt(UIConstants.KEY_NODE_POS, -1);
    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Edit node")
    .setCancelable(true)
//    .setView(myDialogView)
    .setPositiveButton(android.R.string.ok, this)
    .setNegativeButton(android.R.string.cancel, this);

    mBinding = DataBindingUtil.inflate(LayoutInflater.from(builder.getContext()), R.layout.dlg_node_edit_activity, null, false);
    final View myDialogView = mBinding.getRoot();
    builder.setView(mBinding.getRoot());

    final AlertDialog dialog = builder.create();

    mBinding.dlgNodeEditCommon.etNodeLabel.setOnEditorActionListener(this);
    mBinding.rbhAcKind.setOnCheckedChangeListener(this);
    mBinding.editAcHuman.setOnClickListener(this);
    mBinding.editAcService.setOnClickListener(this);

    if (savedInstanceState!=null && savedInstanceState.containsKey(UIConstants.KEY_ACTIVITY)) {
      mActivityNode = DrawableActivity.from(savedInstanceState.<ParcelableActivity>getParcelable(UIConstants.KEY_ACTIVITY), false);
    } else if (getActivity() instanceof NodeEditListener) {
      final NodeEditListener listener = (NodeEditListener) getActivity();
      mActivityNode = (DrawableActivity) listener.getNode(mPos);
      mBinding.setNode(mActivityNode);
    }
    return dialog;
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if (which==DialogInterface.BUTTON_POSITIVE) {
      if (getActivity() instanceof NodeEditListener) {
        final NodeEditListener listener = (NodeEditListener) getActivity();
        final DrawableActivity node = (DrawableActivity) listener.getNode(mPos);
        node.setLabel(mBinding.dlgNodeEditCommon.etNodeLabel.getText().toString());
        node.setMessage(mActivityNode.getMessage());
        node.setResults(mActivityNode.getResults());
        node.setDefines(mActivityNode.getDefines());
        listener.onNodeEdit(mPos);
      }
    }
  }

  @Override
  public void onClick(final View v) {
    switch (v.getId()) {
      case R.id.editAcService:
        break;
      case R.id.editAcHuman: {
        mActivityNode.setLabel(mBinding.dlgNodeEditCommon.etNodeLabel.getText().toString());
        final List<? extends ResultReference> variables = getAccessibleVariables();
        final Intent intent = UserTaskEditorActivity.createIntent(getActivity(),
                                                                  ParcelableActivity.newInstance(mActivityNode, mActivityNode.isCompat()),
                                                                  mActivityNode.getId(), variables);
        startActivityForResult(intent, UIConstants.REQUEST_EDIT_HUMAN);
        break;
      }
    }
  }

  private ArrayList<ResultReference> getAccessibleVariables() {
    final NodeEditListener           listener = (NodeEditListener) getActivity();
    final DrawableProcessNode.Builder        node     = listener.getNode(mPos);
    final List<DrawableProcessNode.Builder>  seen     = new ArrayList<>();
    final ArrayList<ResultReference> result   = new ArrayList<>();
    getAccessibleVariablesFromPredecessors(node, seen, result, listener);

    return result;
  }

  private void getAccessibleVariablesFromPredecessors(final Builder reference,
                                                      final List<DrawableProcessNode.Builder> seen,
                                                      final List<ResultReference> gather,
                                                      final NodeEditListener listener) {
    for(final Identifiable predId: reference.getPredecessors()) {
      final @Nullable DrawableProcessNode.Builder pred = listener.getNode(predId);
      if (! seen.contains(pred)) {
        for(final IXmlResultType result: pred.getResults()) {
          gather.add(VariableReference.newResultReference(pred, result));
        }
        seen.add(pred);
        getAccessibleVariablesFromPredecessors(pred, seen, gather, listener);
      }
    }
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (resultCode==Activity.RESULT_OK) {
      switch (requestCode) {
        case UIConstants.REQUEST_EDIT_HUMAN: {
          final ParcelableActivity newActivity = data.getParcelableExtra(UIConstants.KEY_ACTIVITY);
          if (newActivity!=null) {
            final NodeEditListener listener = (NodeEditListener) getActivity();
            mActivityNode = DrawableActivity.from(newActivity, false);
            mBinding.setNode(mActivityNode);
          }
        }
      }
    }
  }

  @Override
  public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
    if (actionId==EditorInfo.IME_ACTION_DONE) {
      // TODO make a method of this that both this method and onClick call
      onClick(null, DialogInterface.BUTTON_POSITIVE);
      return true;
    }
    return false;
  }


}
