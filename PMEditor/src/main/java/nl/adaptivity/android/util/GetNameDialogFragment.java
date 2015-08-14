package nl.adaptivity.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
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
    public void onClick(DialogInterface pDialog, int pWhich) {
      if (pWhich==DialogInterface.BUTTON_POSITIVE) {
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

  public static interface Callbacks {

    void onNameDialogCompletePositive(GetNameDialogFragment pDialog, int pId, String pString);

    void onNameDialogCompleteNegative(GetNameDialogFragment pDialog, int pId);

  }

  Callbacks mOwner;

  private int mId;

  private EditText mEditText;

  private void setId(int pId) {
    mId = pId;
  }

  public GetNameDialogFragment() { /* empty */}

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (mOwner==null && activity instanceof Callbacks) {
      mOwner = (Callbacks) activity;
    }
  }

  public void setCallbacks(Callbacks callbacks) {
    mOwner = callbacks;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    CharSequence prevName;
    String title;
    String message;
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

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = LayoutInflater.from(builder.getContext());
    View parent = inflater.inflate(R.layout.dialog_content_edit_name, null, false);
    mEditText = (EditText) parent.findViewById(R.id.edit);
//    mEditText = new EditText(getActivity());
    mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
    mEditText.setText(prevName);
    mEditText.selectAll();
    MyClickListener listener = new MyClickListener();

    mEditText.setOnEditorActionListener(listener);
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, listener)
        .setNegativeButton(android.R.string.cancel, listener)
        .setView(parent);
    return builder.create();

  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putCharSequence(KEY_EDIT, mEditText.getText());
  }

  public static GetNameDialogFragment show(FragmentManager pFragmentManager, int pId, String pTitle, String pMessage, Callbacks pCallbacks) {
    return show(pFragmentManager, pId, pTitle, pMessage, pCallbacks, null);
  }

  public static GetNameDialogFragment show(FragmentManager pFragmentManager, int pId, String pTitle, String pMessage, Callbacks pCallbacks, String pPrevious) {
    GetNameDialogFragment f = new GetNameDialogFragment();
    f.setCallbacks(pCallbacks);
    f.setId(pId);
    Bundle args = new Bundle(3);
    if (pTitle!=null) { args.putString(ARG_TITLE, pTitle); }
    if (pMessage!=null) { args.putString(ARG_MESSAGE, pMessage); }
    if (pPrevious!=null) { args.putString(ARG_PREV_NAME, pPrevious); }
    if (args.size()>0) {
      f.setArguments(args);
    }
    f.show(pFragmentManager, "getName");
    return f;
  }
}
