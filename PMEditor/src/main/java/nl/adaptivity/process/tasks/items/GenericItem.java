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

package nl.adaptivity.process.tasks.items;

import android.content.Context;
import android.databinding.Bindable;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import nl.adaptivity.process.editor.android.BR;
import nl.adaptivity.process.editor.android.databinding.TaskitemGenericBinding;

import java.util.List;


public class GenericItem extends LabeledItem implements TextWatcher, OnClickListener {


  private static class ComboFilter extends Filter {

    private List<CharSequence> mOriginal;

    public ComboFilter(final List<CharSequence> original) {
      mOriginal = original;
    }

    @Override
    protected FilterResults performFiltering(final CharSequence constraint) {
      final FilterResults result = new FilterResults();
      result.count=mOriginal.size();
      result.values=mOriginal;
      return result;
    }

    @Override
    protected void publishResults(final CharSequence constraint, final FilterResults results) {
      // We don't change the results.
    }

  }

  private CharSequence mType;
  private ObservableList<CharSequence> mOptions;

  private static class ComboAdapter extends ArrayAdapter<CharSequence> {
    private List<CharSequence> mOriginal;

    public ComboAdapter(final Context context, final List<CharSequence> original) {
      super(context, android.R.layout.simple_dropdown_item_1line, original);
      mOriginal = original;
    }

    @Override
    public Filter getFilter() {
      return new ComboFilter(mOriginal);
    }
  }


  public GenericItem(final CharSequence name, final CharSequence label, final CharSequence type, final CharSequence value, final List<? extends CharSequence> options) {
    super(name, label, value);
    mType = type;
    mOptions = new ObservableArrayList<>();
    if (options!=null && options.size()>0) {
      mOptions.addAll(options);
    }
  }

  @Override
  public Type getType() {
    return Type.GENERIC;
  }


  @Override
  public CharSequence getDBType() {
    return mType;
  }

  @Bindable
  public ObservableList<CharSequence> getOptions() {
    return mOptions;
  }


  public void setOptions(final List<String> options) {
    mOptions = new ObservableArrayList<>();
    if (options!=null && options.size()>0) {
      mOptions.addAll(options);
    }
    notifyPropertyChanged(BR.options);
  }

  @Override
  public void updateView(final ViewDataBinding binding) {
    final TaskitemGenericBinding b = (TaskitemGenericBinding) binding;
    b.setTaskitem(this);
    final AutoCompleteTextView textview = b.taskitemDetailTextText;
    textview.setText(getValue());
    textview.setThreshold(1);
    textview.setAdapter(new ComboAdapter(textview.getContext(), mOptions));
    final Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.setTag(this);
    textview.addTextChangedListener(this);
    textview.setOnClickListener(this);
  }

  @Override
  public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) { /*do nothing*/ }

  @Override
  public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    setValue(s.toString());
  }

  @Override
  public void afterTextChanged(final Editable s) { /*do nothing*/ }

  @Override
  public void onClick(final View v) {
    final AutoCompleteTextView tv = (AutoCompleteTextView) v;
    if (tv.getText().length()==0 &&  !tv.isPopupShowing()) {
      tv.showDropDown();
    }
  }

  @Override
  public boolean isCompleteable() {
    return getValue()!=null;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }

    final GenericItem that = (GenericItem) o;

    if (mType != null ? !mType.equals(that.mType) : that.mType != null) { return false; }
    return mOptions != null ? mOptions.equals(that.mOptions) : that.mOptions == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mType != null ? mType.hashCode() : 0);
    result = 31 * result + (mOptions != null ? mOptions.hashCode() : 0);
    return result;
  }
}