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

package nl.adaptivity.process.editor.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import nl.adaptivity.process.diagram.DrawableJoinSplit;
import nl.adaptivity.process.editor.android.databinding.DlgNodeEditJoinBinding;


public class JoinSplitNodeEditDialogFragment extends DialogFragment implements OnClickListener, OnValueChangeListener, OnCheckedChangeListener, OnEditorActionListener {

  private static final int MAX_MAX = 20;

  public static final String NODE_POS = "node_pos";

  private int mPos=-1;

  private DlgNodeEditJoinBinding mBinding;

  public static JoinSplitNodeEditDialogFragment newInstance(final int position) {
    final JoinSplitNodeEditDialogFragment frag = new JoinSplitNodeEditDialogFragment();
    final Bundle                          args = new Bundle(1);
    args.putInt(NODE_POS, position);
    frag.setArguments(args);
    return frag;
  }

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    mPos = getArguments().getInt(NODE_POS,-1);
    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Edit node")
    .setCancelable(true)
//    .setView(myDialogView)
    .setPositiveButton(android.R.string.ok, this)
    .setNegativeButton(android.R.string.cancel, this);

    mBinding = DataBindingUtil.inflate(LayoutInflater.from(builder.getContext()), R.layout.dlg_node_edit_join, null, false);
    final View myDialogView = mBinding.getRoot();
    builder.setView(mBinding.getRoot());

    final AlertDialog dialog = builder.create();

    mBinding.dlgNodeEditCommon.etNodeLabel.setOnEditorActionListener(this);
    initNumberPicker(mBinding.npMin, 0);
    initNumberPicker(mBinding.npMax, 1);
    mBinding.rgMinmax.setOnCheckedChangeListener(this);

    if (getActivity() instanceof NodeEditListener) {
      final NodeEditListener listener = (NodeEditListener) getActivity();
      final DrawableJoinSplit node = (DrawableJoinSplit) listener.getNode(mPos);
      mBinding.setNode(node);
      setMinMaxEditEnabled(! (node.isAnd()||node.isOr()||node.isXor()));

      if (node.getMin() >= 0) { mBinding.npMin.setValue(node.getMin()); }
      if (node.getMax() >= 1) { mBinding.npMax.setValue(Math.max(node.getMin(), Math.max(1, node.getMax()))); }
    }
    return dialog;
  }

  private void initNumberPicker(final NumberPicker np, final int min) {
    np.setWrapSelectorWheel(false);
    np.setMinValue(min);
    np.setMaxValue(MAX_MAX);
    np.setValue(min);
    np.setOnValueChangedListener(this);
  }

  private void setMinMaxEditEnabled(final boolean enabled) {
    mBinding.setMinMaxEnabled(enabled);
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if (which==DialogInterface.BUTTON_POSITIVE) {
      if (getActivity() instanceof NodeEditListener) {
        final NodeEditListener listener = (NodeEditListener) getActivity();
        final DrawableJoinSplit node = (DrawableJoinSplit) listener.getNode(mPos);
        node.setMin(mBinding.npMin.getValue());
        node.setMax(mBinding.npMax.getValue());
        node.setLabel(mBinding.dlgNodeEditCommon.etNodeLabel.getText().toString());
        listener.onNodeEdit(mPos);
      }
    }
  }

  @Override
  public void onValueChange(final NumberPicker picker, final int oldVal, final int newVal) {
    if (picker.getId()==R.id.np_min) {
      mBinding.npMax.setMinValue(Math.max(1, newVal));
      if (mBinding.npMax.getValue()<mBinding.npMax.getMinValue()) {
        mBinding.npMax.setValue(mBinding.npMax.getMinValue());
      }
    }
  }

  @Override
  public void onCheckedChanged(final RadioGroup group, final int checkedId) {
    final DrawableJoinSplit jsnode = (DrawableJoinSplit) ((NodeEditListener)getActivity()).getNode(mPos);
    final int max = jsnode.getMaxPredecessorCount()==1 ? jsnode.getSuccessors().size() :jsnode.getPredecessors().size();
    final NumberPicker npMin = mBinding.npMin;
    final NumberPicker npMax = mBinding.npMax;

    switch (checkedId) {
      case R.id.radioand:
        npMin.setValue(max);
        npMax.setValue(max);
        npMax.setMinValue(max);
        break;
      case R.id.radioor:
        npMin.setValue(1);
        npMax.setValue(max);
        npMax.setMinValue(1);
        break;
      case R.id.radioxor:
        npMin.setValue(1);
        npMax.setValue(1);
        npMax.setMinValue(1);
        break;
    }
    setMinMaxEditEnabled(checkedId==R.id.radioother);
  }

  @Override
  public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
    if (actionId==EditorInfo.IME_ACTION_DONE) {
      // TODO make a method of this that both call
      onClick(null, DialogInterface.BUTTON_POSITIVE);
      return true;
    }
    return false;
  }

}
