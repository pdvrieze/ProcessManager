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
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import net.devrieze.util.CollectionUtil;
import net.devrieze.util.StringUtil;
import nl.adaptivity.android.dialogs.ComboDialogFragment;
import nl.adaptivity.android.dialogs.DialogResultListener;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.DialogTaskItemBinding;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.ui.UIConstants;
import nl.adaptivity.process.util.CharSequenceDecorator;
import nl.adaptivity.process.util.ModifySequence;
import nl.adaptivity.process.util.VariableReference;
import nl.adaptivity.process.util.VariableReference.ResultReference;
import nl.adaptivity.xml.Namespace;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * A dialog fragment for editing task items. Created by pdvrieze on 04/02/16.
 */
public class ItemEditDialogFragment extends DialogFragment implements OnClickListener, View.OnClickListener, CharSequenceDecorator, DialogResultListener {

  private static final String TAG = "ItemEditDialogFragment";

  public interface ItemEditDialogListener {

    void updateItem(int itemNo, TaskItem newItem);

    void updateDefine(XmlDefineType define);
  }

  public static final int VARSPAN_BORDER_ID = R.drawable.varspan_border;

  private TaskItem mItem;
  private DialogTaskItemBinding mBinding;
  private int mItemNo;
  private ItemEditDialogListener mListener;
  private List<XmlDefineType> mDefines;
  private List<ResultReference> mAvailableVariables;

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
    mAvailableVariables = getArguments().getParcelableArrayList(UIConstants.KEY_VARIABLES);
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
      mItem.setLabel(toItemValue(mBinding.editLabel, mItem.getLabel()));
    }
    if (mBinding.editValue.getVisibility() == View.VISIBLE) {
      mItem.setValue(toItemValue(mBinding.editValue, mItem.getValue()));
    }
  }

  public CharSequence toItemValue(final EditText source, final CharSequence currentValue) {
    final String currentName = mBinding.editName.getText().toString();
    if (! (currentValue instanceof ModifySequence)) {
      if (! hasVariables(source.getText())) {
        return source.getText();
      } else {
        final VariableSpan[] spans     = new VariableSpan[0];
        final XmlDefineType  newDefine = createDefine(getDefineName("d_" + currentName), source.getText());
        mListener.updateDefine(newDefine);
        return ModifySequence.newAttributeSequence("value", newDefine.getName(),null);
      }
    } else { //
      final ModifySequence current = (ModifySequence) currentValue;
      if (! hasVariables(source.getText())) {
        return source.getText(); // Perhaps trigger define purging
      } else {
        XmlDefineType define = getDefine(current.getVariableName());
        define = updateDefine(define, source.getText());
        mListener.updateDefine(define);
        return current;
      }

    }
  }

  private String getDefineName(final String currentName) {
    if (getDefine(currentName)==null) {
      return currentName;
    }
    int i=1;
    String varName;
    while(getDefine((varName = currentName+i))!=null) {
      ++i;
    }
    return varName;
  }

  public XmlDefineType createDefine(final String name, final Spanned annotatedSequence) {
    final XmlDefineType result = new XmlDefineType();
    result.setName(name);

    return updateDefine(result, annotatedSequence);
  }

  @NotNull
  private XmlDefineType updateDefine(final XmlDefineType define, final Spanned annotatedSequence) {
    final CharArrayWriter caw = new CharArrayWriter();
    try {
      final XmlWriter writer = XmlStreaming.newWriter(caw);
      try {
        int       prev           = 0;
        final int sequenceLength = annotatedSequence.length();
        int       next           = annotatedSequence.nextSpanTransition(0, sequenceLength, VariableSpan.class);
        while (next>=0 && next < sequenceLength) {
          writer.text(annotatedSequence.subSequence(prev, next));
          prev = next;

          next = annotatedSequence.nextSpanTransition(prev, sequenceLength, VariableSpan.class);
          Log.d(TAG, "updateDefine getSpans(" + prev + ", " + next + ")");
          final VariableSpan[] spans     = annotatedSequence.getSpans(prev, next, VariableSpan.class);
          final VariableSpan   span      = spans[0]; // no nesting
          VariableReference    reference = span.getReference();
          if (define.getRefNode() == null) { // trick to treat the first result reference different, by using the define's params
            if (reference instanceof ResultReference) {
              final ResultReference resultReference = (ResultReference) reference;
              define.setRefNode(resultReference.getNodeId());
              define.setRefName(resultReference.getVariableName());
              define.setPath(Collections.<Namespace>emptyList(), ".");
              reference = VariableReference.newDefineReference(null, null); // Make this a default reference
            }
          }
          reference.toModifySequence().serialize(writer);
          prev = next;

          next = annotatedSequence.nextSpanTransition(prev, sequenceLength, VariableSpan.class);
        }
        if (prev < sequenceLength) {
          writer.text(annotatedSequence.subSequence(prev, sequenceLength));
        }
      } finally {
        writer.close();
      }
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
    final char[] content = caw.toCharArray();
    define.setContent(Collections.<Namespace>emptyList(), content);
    return define;
  }


  public static boolean hasVariables(final CharSequence string) {
    if (string instanceof Spanned) {
      final Spanned span = (Spanned) string;
      return span.nextSpanTransition(0, span.length(), VariableSpan.class) >=0;
    }
    return false;
  }

  @Override
  public CharSequence decorate(final CharSequence in) {
    if (in instanceof ModifySequence) {
      final ModifySequence sequence = (ModifySequence) in;
      final XmlDefineType define = getDefine(sequence.getVariableName());
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

  private Spanned toSpanned(final XmlReader bodyStreamReader, final XmlDefineType define) throws XmlException {
    return VariableSpan.getSpanned(getActivity(), bodyStreamReader, define, VARSPAN_BORDER_ID);
  }

  private String displayPath(final CharSequence xpath) {
    if (StringUtil.isNullOrEmpty(xpath) || StringUtil.isEqual(".", xpath)) {
      return "";
    }
    return "[" + xpath + ']';
  }

  @Nullable
  private XmlDefineType getDefine(final CharSequence defineName) {
    for (final XmlDefineType candidate : mDefines) {
      if (StringUtil.isEqual(defineName, candidate.getName())) {
        return candidate;
      }
    }
    return null;
  }

  @Override
  public void onClick(final View v) {
    switch (v.getId()) {
      case R.id.btnAddVarLabel: {
        final ComboDialogFragment frag = ComboDialogFragment.newInstance(UIConstants.DIALOG_ID_SELECT_VAR_LABEL, getAllVariables(), getString(R.string.dlgTitleSelectVar));
        frag.show(getFragmentManager(), "dialog");
        break;
      }
      case R.id.btnAddVarValue: {
        final ComboDialogFragment frag = ComboDialogFragment.newInstance(UIConstants.DIALOG_ID_SELECT_VALUE_LABEL, getAllVariables(), getString(R.string.dlgTitleSelectVar));
        frag.show(getFragmentManager(), "dialog");
        break;
      }
    }
  }

  private List<? extends VariableReference> getAllVariables() {
    final ArrayList<VariableReference> allVars = new ArrayList<>();
    allVars.addAll(mAvailableVariables);
    final String currentName = "d_" + mBinding.editName.getText().toString();
    for (final XmlDefineType define : mDefines) {
      if (!(currentName.equals(define.getName()) ||
          (CollectionUtil.isNullOrEmpty(define.getContent()) && (StringUtil.isNullOrEmpty(define.getPath())||".".equals(define.getPath()))))) {
        allVars.add(VariableReference.newDefineReference(define));
      }
    }
    Collections.sort(allVars);
    return allVars;
  }

  @Override
  public void onDialogSuccess(final DialogFragment source, final int id, final Object value) {
    switch (id) {
      case UIConstants.DIALOG_ID_SELECT_VAR_LABEL:
        addVariableSpan(mBinding.editLabel, (VariableReference) value);
        break;
      case UIConstants.DIALOG_ID_SELECT_VALUE_LABEL:
        addVariableSpan(mBinding.editValue, (VariableReference) value);
        break;
    }
  }

  @Override
  public void onDialogCancelled(final DialogFragment source, final int id) {

  }

  private void addVariableSpan(final EditText editText, final VariableReference variableReference) {
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
    final Spanned spanned = VariableSpan.newVarSpanned(getActivity(), null, variableReference, VARSPAN_BORDER_ID);
    editText.getText().replace(start, end, spanned);
  }



  public static ItemEditDialogFragment newInstance(final TaskItem item, final Collection<? extends VariableReference> variables, final List<? extends XmlDefineType> defines, final int itemNo) {
    final ItemEditDialogFragment f = new ItemEditDialogFragment();
    final Bundle args = new Bundle(4);
    args.putInt(UIConstants.KEY_ITEMNO, itemNo);
    args.putString(UIConstants.KEY_ITEM, XmlUtil.toString(item));
    args.putStringArrayList(UIConstants.KEY_DEFINES, XmlUtil.toString(defines));
    args.putParcelableArrayList(UIConstants.KEY_VARIABLES, CollectionUtil.toArrayList(variables));
    f.setArguments(args);
    return f;
  }
}
