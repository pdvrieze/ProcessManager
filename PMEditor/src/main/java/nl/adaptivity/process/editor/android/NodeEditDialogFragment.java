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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import nl.adaptivity.process.diagram.DrawableJoinSplit;
import nl.adaptivity.process.diagram.DrawableProcessNode;


public class NodeEditDialogFragment extends DialogFragment implements OnClickListener, OnValueChangeListener, OnCheckedChangeListener, OnEditorActionListener {

  private static final int MAX_MAX = 20;

  public static final String NODE_POS = "node_pos";

  private int mPos=-1;

  private EditText mEtLabel;

  private RadioGroup mRgMinMax;

  private ViewGroup mVgMinMax;

  private NumberPicker mNpMin;

  private NumberPicker mNpMax;

  private TextView mLblLabel;

  public static NodeEditDialogFragment newInstance(final int position) {
    NodeEditDialogFragment frag = new NodeEditDialogFragment();
    Bundle args = new Bundle(1);
    args.putInt(NODE_POS, position);
    frag.setArguments(args);
    return frag;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    mPos = getArguments().getInt(NODE_POS,-1);
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Edit node")
    .setCancelable(true)
//    .setView(myDialogView)
    .setPositiveButton(android.R.string.ok, this)
    .setNegativeButton(android.R.string.cancel, this);

    @SuppressLint("InflateParams")
    View myDialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.dlg_node_edit, null);
    builder.setView(myDialogView);

    final AlertDialog dialog = builder.create();
//    FrameLayout container = (FrameLayout) dialog.findViewById(android.R.id.custom);
//
//    container.addView(myDialogView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    mNpMin = (NumberPicker) myDialogView.findViewById(R.id.np_min);
    mNpMax = (NumberPicker) myDialogView.findViewById(R.id.np_max);
    mRgMinMax = (RadioGroup) myDialogView.findViewById(R.id.rg_minmax);
    mVgMinMax = (ViewGroup) myDialogView.findViewById(R.id.vg_minmax);
    mEtLabel = (EditText) myDialogView.findViewById(R.id.et_node_label);
    mLblLabel = (TextView) myDialogView.findViewById(R.id.lbl_node_label);

    mEtLabel.setOnEditorActionListener(this);
    initNumberPicker(mNpMin, 0);
    initNumberPicker(mNpMax, 1);
    mRgMinMax.setOnCheckedChangeListener(this);

    if (getActivity() instanceof NodeEditListener) {
      NodeEditListener listener = (NodeEditListener) getActivity();
      DrawableProcessNode node = listener.getNode(mPos);
      mEtLabel.setText(node.getLabel());
      mRgMinMax.setVisibility(View.GONE);
      mVgMinMax.setVisibility(View.GONE);
    }
    if (mEtLabel.getVisibility()!=View.VISIBLE) {
      // Don't display the keyboard initially if we can't edit the label
      dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
    }
    return dialog;
  }

  private void setEditLabelEnabled(boolean enabled) {
    final int visibility = enabled ? View.VISIBLE : View.GONE;
    mEtLabel.setVisibility(visibility);
    mLblLabel.setVisibility(visibility);
  }

  private void initNumberPicker(NumberPicker np, int min) {
    np.setWrapSelectorWheel(false);
    np.setMinValue(min);
    np.setMaxValue(MAX_MAX);
    np.setValue(min);
    np.setOnValueChangedListener(this);
  }

  private void setMinMaxEditEnabled(boolean enabled) {
    mVgMinMax.setEnabled(enabled);
    mNpMin.setEnabled(enabled);
    mNpMax.setEnabled(enabled);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which==DialogInterface.BUTTON_POSITIVE) {
      if (getActivity() instanceof NodeEditListener) {
        NodeEditListener listener = (NodeEditListener) getActivity();
        DrawableProcessNode node = listener.getNode(mPos);
        if (node instanceof DrawableJoinSplit) {
          DrawableJoinSplit jsnode = (DrawableJoinSplit) node;
          jsnode.setMin(mNpMin.getValue());
          jsnode.setMax(mNpMax.getValue());
        }
        node.setLabel(mEtLabel.getText().toString());
        listener.onNodeEdit(mPos);
      }
    }
  }

  @Override
  public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
    if (picker.getId()==R.id.np_min) {
      mNpMax.setMinValue(Math.max(1, newVal));
      if (mNpMax.getValue()<mNpMax.getMinValue()) {
        mNpMax.setValue(mNpMax.getMinValue());
      }
    }
  }

  @Override
  public void onCheckedChanged(RadioGroup group, int checkedId) {
    DrawableJoinSplit jsnode = (DrawableJoinSplit) ((NodeEditListener)getActivity()).getNode(mPos);
    int max = jsnode.getMaxPredecessorCount()==1 ? jsnode.getSuccessors().size() :jsnode.getPredecessors().size();

    switch (checkedId) {
      case R.id.radioand:
        mNpMin.setValue(max);
        mNpMax.setValue(max);
        mNpMax.setMinValue(max);
        break;
      case R.id.radioor:
        mNpMin.setValue(1);
        mNpMax.setValue(max);
        mNpMax.setMinValue(1);
        break;
      case R.id.radioxor:
        mNpMin.setValue(1);
        mNpMax.setValue(1);
        mNpMax.setMinValue(1);
        break;
    }
    setMinMaxEditEnabled(checkedId==R.id.radioother);
  }

  @Override
  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    if (actionId==EditorInfo.IME_ACTION_DONE) {
      // TODO make a method of this that both call
      onClick(null, DialogInterface.BUTTON_POSITIVE);
      return true;
    }
    return false;
  }

}
