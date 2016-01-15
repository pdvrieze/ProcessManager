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
import nl.adaptivity.process.editor.android.databinding.DlgNodeEditActivityBinding;
import nl.adaptivity.process.ui.activity.UserTaskEditorActivity;


/**
 * A dialogfragment for editing activity nodes.
 */
public class ActivityEditDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, OnEditorActionListener, OnCheckedChangeListener, View.OnClickListener {

  private static final int MAX_MAX = 20;

  public static final String NODE_POS = "node_pos";
  public static final int REQUEST_EDIT_HUMAN = 12;

  private int mPos=-1;

  private DlgNodeEditActivityBinding mBinding;

  @Override
  public void onCheckedChanged(final RadioButtonHelper source, final int oldCheckedId, final int newCheckedId) {
    mBinding.editAcHuman.setEnabled(newCheckedId==R.id.radio_ac_human);
    mBinding.editAcService.setEnabled(newCheckedId==R.id.radio_ac_service);
  }

  public static ActivityEditDialogFragment newInstance(final int position) {
    ActivityEditDialogFragment frag = new ActivityEditDialogFragment();
    Bundle args = new Bundle(1);
    args.putInt(NODE_POS, position);
    frag.setArguments(args);
    return frag;
  }

  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
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

    mBinding = DataBindingUtil.inflate(LayoutInflater.from(builder.getContext()), R.layout.dlg_node_edit_activity, null, false);
    final View myDialogView = mBinding.getRoot();
    builder.setView(mBinding.getRoot());

    final AlertDialog dialog = builder.create();

    mBinding.dlgNodeEditCommon.etNodeLabel.setOnEditorActionListener(this);
    mBinding.rbhAcKind.setOnCheckedChangeListener(this);
    mBinding.editAcHuman.setOnClickListener(this);
    mBinding.editAcService.setOnClickListener(this);

    if (getActivity() instanceof NodeEditListener) {
      final NodeEditListener listener = (NodeEditListener) getActivity();
      final DrawableActivity node = (DrawableActivity) listener.getNode(mPos);
      mBinding.setNode(node);
    }
    return dialog;
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if (which==DialogInterface.BUTTON_POSITIVE) {
      if (getActivity() instanceof NodeEditListener) {
        final NodeEditListener listener = (NodeEditListener) getActivity();
        final DrawableActivity node = (DrawableActivity) listener.getNode(mPos);
        listener.onNodeEdit(mPos);
      }
    }
  }

  @Override
  public void onClick(final View v) {
    switch (v.getId()) {
      case R.id.editAcService:
        break;
      case R.id.editAcHuman:
        startActivityForResult(new Intent(getActivity(), UserTaskEditorActivity.class), REQUEST_EDIT_HUMAN);
        break;
    }
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
