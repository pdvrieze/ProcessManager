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

import android.text.Editable;
import android.text.TextWatcher;



public abstract class TextLabeledItem extends LabeledItem implements TextWatcher {

  public TextLabeledItem(final CharSequence name, final CharSequence label, final CharSequence value) {
    super(name, label, value);
  }

  @Override
  public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) { /*do nothing*/ }

  @Override
  public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    setValue(s.toString());
  }

  @Override
  public void afterTextChanged(final Editable s) { /*do nothing*/ }

}
