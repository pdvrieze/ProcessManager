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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.tasks.items;

import android.databinding.ViewDataBinding;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import nl.adaptivity.process.editor.android.databinding.TaskitemTextBinding;

import java.util.ArrayList;
import java.util.List;


public class TextItem extends TextLabeledItem {

  private final List<CharSequence>         mSuggestions;
  private       ArrayAdapter<CharSequence> mSuggestionAdapter;

  public TextItem(final CharSequence name, final CharSequence label, final CharSequence value, final List<? extends CharSequence> suggestions) {
    super(name, label, value);
    mSuggestions = new ArrayList<>(suggestions);
  }

  @Override
  public void updateView(final ViewDataBinding binding) {
    final TaskitemTextBinding b = (TaskitemTextBinding) binding;
    b.setTaskitem(this);
    final AutoCompleteTextView textview = (AutoCompleteTextView) b.taskitemDetailTextText;
    textview.setText(getValue());
    if (mSuggestions!=null && mSuggestions.size()>0) {
      if (mSuggestionAdapter == null) {
        mSuggestionAdapter = new ArrayAdapter<>(textview.getContext(), android.R.layout.simple_dropdown_item_1line, mSuggestions);
      }
      textview.setAdapter(mSuggestionAdapter);
    }

    final Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.addTextChangedListener(this);
    textview.setTag(this);
  }

  @Override
  public Type getType() {
    return Type.TEXT;
  }

  @Override
  public boolean isCompleteable() {
    return getValue()!=null && getValue().length()>0;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }

    final TextItem textItem = (TextItem) o;

    if (mSuggestions != null ? !mSuggestions.equals(textItem.mSuggestions) : textItem.mSuggestions != null) {
      return false;
    }
    return mSuggestionAdapter != null ? mSuggestionAdapter.equals(textItem.mSuggestionAdapter) : textItem.mSuggestionAdapter == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mSuggestions != null ? mSuggestions.hashCode() : 0);
    result = 31 * result + (mSuggestionAdapter != null ? mSuggestionAdapter.hashCode() : 0);
    return result;
  }
}
