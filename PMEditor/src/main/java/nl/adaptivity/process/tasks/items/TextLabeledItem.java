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

  public TextLabeledItem(String name, String label, String value) {
    super(name, label, value);
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*do nothing*/ }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    setValue(s.toString());
  }

  @Override
  public void afterTextChanged(Editable s) { /*do nothing*/ }

}
