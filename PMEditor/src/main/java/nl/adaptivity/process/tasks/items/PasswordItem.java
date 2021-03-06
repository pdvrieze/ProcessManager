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
import android.widget.TextView;
import nl.adaptivity.process.editor.android.databinding.TaskitemPasswordBinding;


public class PasswordItem extends TextLabeledItem {

  public PasswordItem(final CharSequence name, final CharSequence label, final CharSequence value) {
    super(name, label, value);
  }

  @Override
  public Type getType() {
    return Type.PASSWORD;
  }

  @Override
  public void updateView(final ViewDataBinding binding) {
    final TaskitemPasswordBinding b = (TaskitemPasswordBinding) binding;
    b.setTaskitem(this);
    final TextView textview = b.taskitemDetailTextText;
    textview.setText(getValue());
    final Object tag = textview.getTag();
    if (tag instanceof TextWatcher) {
      textview.removeTextChangedListener((TextWatcher) tag);
    }
    textview.addTextChangedListener(this);
    textview.setTag(this);
  }

  @Override
  public boolean isCompleteable() {
    return getValue()!=null;
  }
}
