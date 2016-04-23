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

package nl.adaptivity.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.widget.Spinner;
import net.devrieze.util.CollectionUtil;
import nl.adaptivity.android.widget.ComboAdapter;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.ui.UIConstants;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pdvrieze on 16/02/16.
 */
public class ComboDialogFragment extends DialogFragment implements OnClickListener{

  private ArrayList<? extends Parcelable> mValues;
  private CharSequence mTitle;
  private DialogResultListener mListener;
  private int mDialogId;
  private Spinner mComboView;


  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mValues = getArguments().getParcelableArrayList(UIConstants.KEY_VALUES);
    mTitle = getArguments().getCharSequence(UIConstants.KEY_TITLE);
    mDialogId = getArguments().getInt(UIConstants.KEY_DIALOG_ID);
  }

  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    if (activity instanceof DialogResultListener) {
      mListener = (DialogResultListener) activity;
    }
  }

  @SuppressLint("InflateParams")
  @NonNull
  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    AlertDialog.Builder builder = new Builder(getActivity());
    builder.setTitle(mTitle);
    mComboView = (Spinner) LayoutInflater.from(builder.getContext()).inflate(R.layout.dlg_combo_view, null, false);
    mComboView.setAdapter(new ComboAdapter(builder.getContext(), mValues));
    builder.setView(mComboView)
           .setCancelable(true)
           .setPositiveButton(android.R.string.ok, this)
           .setNegativeButton(android.R.string.cancel, this);

    return builder.create();
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if (mListener!=null) {
      switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
          mListener.onDialogSuccess(this, mDialogId, mComboView.getSelectedItem());
          break;
        case DialogInterface.BUTTON_NEGATIVE:
          mListener.onDialogCancelled(this, mDialogId);
          break;
      }
    }
  }

  public static ComboDialogFragment newInstance(int dialogId, List<? extends Parcelable> values, CharSequence title) {
    Bundle args = new Bundle(2);
    args.putParcelableArrayList(UIConstants.KEY_VALUES, CollectionUtil.toArrayList(values));
    args.putCharSequence(UIConstants.KEY_TITLE, title);
    args.putInt(UIConstants.KEY_DIALOG_ID, dialogId);
    ComboDialogFragment fragment = new ComboDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }
}
