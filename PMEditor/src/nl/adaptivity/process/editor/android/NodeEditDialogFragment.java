package nl.adaptivity.process.editor.android;

import nl.adaptivity.process.diagram.DrawableJoinSplit;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class NodeEditDialogFragment extends DialogFragment implements OnClickListener, OnValueChangeListener, OnCheckedChangeListener, OnEditorActionListener {

  private static final int MAX_MAX = 20;

  public interface NodeEditListener {
    public DrawableProcessNode getNode(int pos);
    public void onNodeEdit(int pos);
  }

  public static final String NODE_POS = "node_pos";

  private int mPos=-1;

  private EditText mEtLabel;

  private RadioGroup mRgMinMax;

  private ViewGroup mVgMinMax;

  private NumberPicker mNpMin;

  private NumberPicker mNpMax;

  private TextView mLblLabel;

  @Override
  public void onAttach(Activity pActivity) {
    super.onAttach(pActivity);
  }

  @Override
  public Dialog onCreateDialog(Bundle pSavedInstanceState) {
    mPos = getArguments().getInt(NODE_POS,-1);
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Edit node")
    .setCancelable(true)
//    .setView(myDialogView)
    .setPositiveButton(android.R.string.ok, this)
    .setNegativeButton(android.R.string.cancel, this);

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
      if (node instanceof DrawableJoinSplit) {
        setEditLabelEnabled(false);
        setMinMaxEditEnabled(false);
        DrawableJoinSplit jsnode = (DrawableJoinSplit) node;
        int max = jsnode.getMaxPredecessorCount()==1 ? jsnode.getSuccessors().size() :jsnode.getPredecessors().size();
        if (jsnode.getMin()==1) {
          if (jsnode.getMax()==1) {
            mRgMinMax.check(R.id.radioxor);
          } else if (jsnode.getMax()>=max) {
            mRgMinMax.check(R.id.radioor);
          } else {
            mRgMinMax.check(R.id.radioother);
          }
        } else if (jsnode.getMin()==jsnode.getMax()&& jsnode.getMin()>=max) {
          mRgMinMax.check(R.id.radioand);
        } else {
          mRgMinMax.check(R.id.radioother);
          mVgMinMax.setEnabled(true);
        }
        if (jsnode.getMin()>=0) { mNpMin.setValue(jsnode.getMin()); }
        if (jsnode.getMax()>=1) { mNpMax.setValue(Math.max(jsnode.getMin(),Math.max(1,jsnode.getMax()))); }
      } else {
        mEtLabel.setText(node.getLabel());
        mRgMinMax.setVisibility(View.GONE);
        mVgMinMax.setVisibility(View.GONE);
      }
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
  public void onClick(DialogInterface pDialog, int which) {
    if (which==DialogInterface.BUTTON_POSITIVE) {
      if (getActivity() instanceof NodeEditListener) {
        NodeEditListener listener = (NodeEditListener) getActivity();
        DrawableProcessNode node = listener.getNode(mPos);
        if (node instanceof DrawableJoinSplit) {
          DrawableJoinSplit jsnode = (DrawableJoinSplit) node;
          jsnode.setMin(mNpMin.getValue());
          jsnode.setMax(mNpMax.getValue());
        } else {
          node.setLabel(mEtLabel.getText().toString());
        }
        listener.onNodeEdit(mPos);
      }
    }
  }

  @Override
  public void onValueChange(NumberPicker pPicker, int pOldVal, int pNewVal) {
    if (pPicker.getId()==R.id.np_min) {
      mNpMax.setMinValue(Math.max(1, pNewVal));
      if (mNpMax.getValue()<mNpMax.getMinValue()) {
        mNpMax.setValue(mNpMax.getMinValue());
      }
    }
  }

  @Override
  public void onCheckedChanged(RadioGroup pGroup, int pCheckedId) {
    DrawableJoinSplit jsnode = (DrawableJoinSplit) ((NodeEditListener)getActivity()).getNode(mPos);
    int max = jsnode.getMaxPredecessorCount()==1 ? jsnode.getSuccessors().size() :jsnode.getPredecessors().size();

    switch (pCheckedId) {
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
    setMinMaxEditEnabled(pCheckedId==R.id.radioother);
  }

  @Override
  public boolean onEditorAction(TextView pV, int pActionId, KeyEvent pEvent) {
    if (pActionId==EditorInfo.IME_ACTION_DONE) {
      // TODO make a method of this that both call
      onClick(null, DialogInterface.BUTTON_POSITIVE);
      return true;
    }
    return false;
  }

}
