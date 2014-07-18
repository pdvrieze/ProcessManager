package nl.adaptivity.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

public class GetNameDialogFragment extends DialogFragment {


  private class MyClickListener implements OnClickListener {

    private EditText mEditText;

    public MyClickListener(EditText pEditText) {
      mEditText = pEditText;
    }

    @Override
    public void onClick(DialogInterface pDialog, int pWhich) {
      if (pWhich==DialogInterface.BUTTON_POSITIVE) {
        mOwner.onNameDialogCompletePositive(GetNameDialogFragment.this, mEditText.getText().toString());
      } else {
        mOwner.onNameDialogCompleteNegative(GetNameDialogFragment.this);
      }

    }

  }

  private static final String ARG_MESSAGE = "message";

  private static final String ARG_TITLE = "title";

  public static final String ARG_PREV_NAME = "prevName";

  private static final String DEFAULT_TITLE = "Give name";

  private static final String DEFAULT_MESSAGE = "Provide the new name";

  public static interface Callbacks {

    void onNameDialogCompletePositive(GetNameDialogFragment pDialog, String pString);

    void onNameDialogCompleteNegative(GetNameDialogFragment pDialog);

  }

  Callbacks mOwner;

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
    String prevName;
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

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    final EditText editText = new EditText(getActivity());
    editText.setInputType(InputType.TYPE_CLASS_TEXT);
    editText.setText(prevName);
    editText.selectAll();
    MyClickListener listener = new MyClickListener(editText);
    builder.setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, listener)
        .setNegativeButton(android.R.string.cancel, listener)
        .setView(editText);
    return builder.create();

  }

  public static GetNameDialogFragment show(FragmentManager pFragmentManager, String pTitle, String pMessage, Callbacks pCallbacks) {
    return show(pFragmentManager, pTitle, pMessage, pCallbacks, null);
  }

  public static GetNameDialogFragment show(FragmentManager pFragmentManager, String pTitle, String pMessage, Callbacks pCallbacks, String pPrevious) {
    GetNameDialogFragment f = new GetNameDialogFragment();
    f.setCallbacks(pCallbacks);
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
