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

package nl.adaptivity.process.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.DialogTaskItemBinding;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;


/**
 * A dialog fragment for editing task items. Created by pdvrieze on 04/02/16.
 */
public class ItemEditDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

  public interface ItemEditDialogListener {

    void updateItem(int itemNo, TaskItem newItem);
  }

  private TaskItem mItem;
  private DialogTaskItemBinding mBinding;
  private int mItemNo;
  private ItemEditDialogListener mListener;

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      mItemNo = getArguments().getInt(UIConstants.KEY_ITEMNO);
      if (savedInstanceState != null && savedInstanceState.containsKey(UIConstants.KEY_ITEM)) {
        mItem = XmlUtil.deSerialize(savedInstanceState.getString(UIConstants.KEY_ITEM), TaskItem.class);
      } else {
        mItem = XmlUtil.deSerialize(getArguments().getString(UIConstants.KEY_ITEM), TaskItem.class);
      }
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }

  }

  @NonNull
  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.dlg_title_edit_taskitem);

    final LayoutInflater inflater = LayoutInflater.from(builder.getContext());
    mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_task_item, null, false);
    mBinding.setItem(mItem);
    mBinding.setHideTitle(true);

    builder.setCancelable(true)
           .setView(mBinding.getRoot())
           .setPositiveButton(android.R.string.ok, this)
           .setNegativeButton(android.R.string.cancel, this);
    return builder.create();
  }

  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    mListener = (ItemEditDialogListener) activity;
  }
//  @Nullable
//  @Override
//  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
//    mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_task_item, container, false);
//    mBinding.setItem(mItem);
//    return mBinding.getRoot();
//  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    updateItemFromUI();
    outState.putString(UIConstants.KEY_ITEM, XmlUtil.toString(mItem));
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if (which == DialogInterface.BUTTON_POSITIVE) {
      updateItemFromUI();
      mListener.updateItem(mItemNo, mItem);
    }
  }

  public void updateItemFromUI() {
    mItem.setName(mBinding.editName.getText().toString());
    if (mBinding.editLabel.getVisibility() == View.VISIBLE) {
      mItem.setLabel(mBinding.editLabel.getText().toString());
    }
    if (mBinding.editValue.getVisibility() == View.VISIBLE) {
      mItem.setValue(mBinding.editValue.getText().toString());
    }
  }

  public static ItemEditDialogFragment newInstance(TaskItem item, int itemNo) {
    ItemEditDialogFragment f = new ItemEditDialogFragment();
    Bundle args = new Bundle(2);
    args.putInt(UIConstants.KEY_ITEMNO, itemNo);
    args.putString(UIConstants.KEY_ITEM, XmlUtil.toString(item));
    f.setArguments(args);
    return f;
  }
}
