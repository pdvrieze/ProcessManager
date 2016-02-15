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
import android.content.DialogInterface.OnClickListener;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.DialogTaskItemBinding;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.process.util.CharSequenceDecorator;
import nl.adaptivity.process.util.ModifyHelper;
import nl.adaptivity.process.util.ModifySequence;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming.EventType;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * A dialog fragment for editing task items. Created by pdvrieze on 04/02/16.
 */
public class ItemEditDialogFragment extends DialogFragment implements OnClickListener, View.OnClickListener, CharSequenceDecorator {

  public interface ItemEditDialogListener {

    void updateItem(int itemNo, TaskItem newItem);
  }

  public static final int VARSPAN_BORDER_ID = R.drawable.varspan_border;

  private TaskItem mItem;
  private DialogTaskItemBinding mBinding;
  private int mItemNo;
  private ItemEditDialogListener mListener;
  private List<XmlDefineType> mDefines;

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
      mDefines = XmlUtil.deSerialize(getArguments().getStringArrayList(UIConstants.KEY_DEFINES), XmlDefineType.class);
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
    mBinding.setDecorator(this);
    mBinding.setHideTitle(true);
    mBinding.btnAddVarLabel.setOnClickListener(this);
    mBinding.btnAddVarValue.setOnClickListener(this);

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

  @Override
  public CharSequence decorate(final CharSequence in) {
    if (in instanceof ModifySequence) {
      ModifySequence sequence = (ModifySequence) in;
      XmlDefineType define = getDefine(sequence.getDefineName());
      if (define==null) {
        throw new IllegalArgumentException("Invalid state");
      }
      try {
        return toSpanned(define.getBodyStreamReader(), define);
      } catch (XmlException e) {
        throw new RuntimeException(e);
      }

    } else {
      return in;
    }
  }

  private Spanned toSpanned(final XmlReader bodyStreamReader, XmlDefineType define) throws XmlException {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    while (bodyStreamReader.hasNext()) {
      switch (bodyStreamReader.next()) {
        case CDSECT:
        case TEXT:
          builder.append(bodyStreamReader.getText());
          break;
        case START_ELEMENT: {
          CharSequence elemNS = bodyStreamReader.getNamespaceUri();
          CharSequence elemLN = bodyStreamReader.getLocalName();
          ModifySequence var = ModifyHelper.parseAny(bodyStreamReader);
          bodyStreamReader.require(EventType.END_ELEMENT, elemNS, elemLN);
          String displayName = getDisplayName(define, var);
          builder.append(VariableSpan.newVarSpanned(getActivity(), displayName, VARSPAN_BORDER_ID));
          break;
        }
        case END_DOCUMENT:
          return new SpannedString(builder);
        default:
          XmlUtil.unhandledEvent(bodyStreamReader);
      }
    }
    return new SpannedString(builder);
  }

  private String getDisplayName(final XmlDefineType baseDefine, final ModifySequence var) {
    XmlDefineType define;
    if (var.getDefineName()==null) {
      define = baseDefine;
    } else {
      define = getDefine(var.getDefineName());
    }
    // If the define is a standard define, importing a variable from another activity
    if ((define==baseDefine || define.getContent()==null || define.getContent().length==0) &&
        (StringUtil.isNullOrEmpty(baseDefine.getPath()) || StringUtil.isEqual(".", define.getPath()))) {
      return "$"+define.getRefNode()+"."+define.getRefName()+displayPath(var.getXpath());
    }
    return "@"+(var.getDefineName()==null ? baseDefine.getRefName()+'.'+baseDefine.getRefNode() : var.getDefineName()) +var.getDefineName()+displayPath(var.getXpath());
  }

  private String displayPath(final CharSequence xpath) {
    if (StringUtil.isNullOrEmpty(xpath) || StringUtil.isEqual(".", xpath)) {
      return "";
    }
    return "["+xpath+"]";
  }

  @Nullable
  private XmlDefineType getDefine(final CharSequence defineName) {
    for (XmlDefineType candidate : mDefines) {
      if (StringUtil.isEqual(defineName, candidate.getName())) {
        return candidate;
      }
    }
    return null;
  }

  @Override
  public void onClick(final View v) {
    switch (v.getId()) {
      case R.id.btnAddVarLabel:
        addVariableSpan(mBinding.editLabel);
        break;
      case R.id.btnAddVarValue:
        addVariableSpan(mBinding.editValue);
        break;
    }
  }

  private void addVariableSpan(final EditText editText) {
    final int start;
    final int end;
    if (editText.length()==0) {
      start = 0; end = 0;
    } else if (editText.getSelectionStart()<0) {
      start=editText.length();
      end = start;
    } else {
      start = Math.min(editText.getSelectionStart(), editText.getSelectionEnd());
      end = Math.max(editText.getSelectionStart(), editText.getSelectionEnd());
    }
    final Spanned spanned = VariableSpan.newVarSpanned(getActivity(), "foo", VARSPAN_BORDER_ID);
    editText.getText().replace(start, end, spanned);
  }



  public static ItemEditDialogFragment newInstance(TaskItem item, final List<? extends XmlDefineType> defines, int itemNo) {
    ItemEditDialogFragment f = new ItemEditDialogFragment();
    Bundle args = new Bundle(2);
    args.putInt(UIConstants.KEY_ITEMNO, itemNo);
    args.putString(UIConstants.KEY_ITEM, XmlUtil.toString(item));
    args.putStringArrayList(UIConstants.KEY_DEFINES, XmlUtil.toString(defines));
    f.setArguments(args);
    return f;
  }
}
