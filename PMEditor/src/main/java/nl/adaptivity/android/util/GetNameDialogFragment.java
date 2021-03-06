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

package nl.adaptivity.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import nl.adaptivity.process.editor.android.R;


public class GetNameDialogFragment extends DialogFragment {


  private class MyClickListener implements OnClickListener, OnEditorActionListener {

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
      if (which==DialogInterface.BUTTON_POSITIVE) {
        mOwner.onNameDialogCompletePositive(GetNameDialogFragment.this, mId, mEditText.getText().toString());
      } else {
        mOwner.onNameDialogCompleteNegative(GetNameDialogFragment.this, mId);
      }

    }

    @Override
    public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
      if (actionId== EditorInfo.IME_ACTION_GO) {
        mOwner.onNameDialogCompletePositive(GetNameDialogFragment.this, mId, mEditText.getText().toString());
        getDialog().dismiss();
        return true;
      }
      return false;
    }
  }

  private static final String ARG_MESSAGE = "message";

  private static final String ARG_TITLE = "title";

  public static final String ARG_PREV_NAME = "prevName";

  private static final String DEFAULT_TITLE = "Give name";

  private static final String DEFAULT_MESSAGE = "Provide the new name";

  private static final String KEY_EDIT = "text";

  public interface GetNameDialogFragmentCallbacks {

    void onNameDialogCompletePositive(GetNameDialogFragment dialog, int id, String string);

    void onNameDialogCompleteNegative(GetNameDialogFragment dialog, int id);

  }

  GetNameDialogFragmentCallbacks mOwner;

  private int mId;

  private EditText mEditText;

  private void setId(final int id) {
    mId = id;
  }

  public GetNameDialogFragment() { /* empty */}

  @Override
  public void onAttach(final Activity activity) {
    super.onAttach(activity);
    if (mOwner==null && activity instanceof GetNameDialogFragmentCallbacks) {
      mOwner = (GetNameDialogFragmentCallbacks) activity;
    }
  }

  public void setCallbacks(final GetNameDialogFragmentCallbacks callbacks) {
    mOwner = callbacks;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
    CharSequence prevName;
    final String title;
    final String message;
    if (getArguments()==null) {
      prevName = null;
      title = DEFAULT_TITLE;
      message = DEFAULT_MESSAGE;
    } else {
      prevName = getArguments().getString(ARG_PREV_NAME);
      title = getArguments().containsKey(ARG_TITLE) ? getArguments().getString(ARG_TITLE) : DEFAULT_TITLE;
      message = getArguments().containsKey(ARG_MESSAGE) ? getArguments().getString(ARG_MESSAGE) : DEFAULT_MESSAGE;
    }
    if (savedInstanceState!=null && savedInstanceState.containsKey(KEY_EDIT)) {
      prevName = savedInstanceState.getCharSequence(KEY_EDIT);
    }

    final AlertDialog.Builder                 builder  = new AlertDialog.Builder(getActivity());
    final LayoutInflater                      inflater = LayoutInflater.from(builder.getContext());
    @SuppressLint("InflateParams") final View parent   = inflater.inflate(R.layout.dialog_content_edit_name, null, false);
    mEditText = (EditText) parent.findViewById(R.id.edit);
//    mEditText = new EditText(getActivity());
    mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
    mEditText.setText(prevName);
    mEditText.selectAll();
    final MyClickListener listener = new MyClickListener();

    mEditText.setOnEditorActionListener(listener);
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, listener)
        .setNegativeButton(android.R.string.cancel, listener)
        .setView(parent);
    return builder.create();

  }

  @Override
  public void onSaveInstanceState(@NonNull final Bundle outState) {
    outState.putCharSequence(KEY_EDIT, mEditText.getText());
  }

  @NonNull
  public static GetNameDialogFragment show(@NonNull final FragmentManager fragmentManager, final int id, final String title, final String message, final GetNameDialogFragmentCallbacks callbacks) {
    return show(fragmentManager, id, title, message, callbacks, null);
  }

  @NonNull
  public static GetNameDialogFragment show(@NonNull final FragmentManager fragmentManager, final int id, @Nullable final String title, @Nullable
  final String message, final GetNameDialogFragmentCallbacks callbacks, @Nullable final String previous) {
    final GetNameDialogFragment f = new GetNameDialogFragment();
    f.setCallbacks(callbacks);
    f.setId(id);
    final Bundle args = new Bundle(3);
    if (title!=null) { args.putString(ARG_TITLE, title); }
    if (message!=null) { args.putString(ARG_MESSAGE, message); }
    if (previous!=null) { args.putString(ARG_PREV_NAME, previous); }
    if (args.size()>0) {
      f.setArguments(args);
    }
    f.show(fragmentManager, "getName");
    return f;
  }
}
