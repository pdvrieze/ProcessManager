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

import android.databinding.ViewDataBinding;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import nl.adaptivity.process.editor.android.databinding.TaskitemListBinding;

import java.util.ArrayList;
import java.util.List;


public class ListItem extends LabeledItem implements OnItemSelectedListener {

  private List<CharSequence> mOptions;

  public ListItem(final CharSequence name, final CharSequence label, final CharSequence value, final List<? extends CharSequence> options) {
    super(name, label, value);
    mOptions = new ArrayList<>(options);
  }

  @Override
  public Type getType() {
    return Type.LIST;
  }

  @Override
  public void updateView(final ViewDataBinding binding) {
    final TaskitemListBinding b = (TaskitemListBinding) binding;
    b.setTaskitem(this);
    final Spinner      view  = b.taskitemDetailList;
    final CharSequence value = getValue();
    int                index = AdapterView.INVALID_POSITION;
    if (value!=null) {
      if (mOptions == null) {
        mOptions=new ArrayList<>(1);
        mOptions.add(value);
        index = 0;
      } else {
        index = mOptions.indexOf(value);
        if (index<0) {
          mOptions.add(value);
          index = mOptions.size()-1;
        }
      }
    }
    view.setAdapter(mOptions==null ? null : new ArrayAdapter<>(view.getContext(), android.R.layout.simple_dropdown_item_1line, mOptions));
    view.setSelection(index, false);

    view.setOnItemSelectedListener(this);
  }

  @Override
  public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
    setValue(mOptions.get(position));
  }

  @Override
  public void onNothingSelected(final AdapterView<?> parent) {
    setValue(null);
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

    final ListItem listItem = (ListItem) o;

    return mOptions != null ? mOptions.equals(listItem.mOptions) : listItem.mOptions == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mOptions != null ? mOptions.hashCode() : 0);
    return result;
  }
}
