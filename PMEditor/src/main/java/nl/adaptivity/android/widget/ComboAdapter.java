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

package nl.adaptivity.android.widget;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.List;


/**
 * Created by pdvrieze on 16/02/16.
 */
public class ComboAdapter<T> extends ArrayAdapter<T> {

  private final List<T> mList;

  public ComboAdapter(final Context context, final List<T> list) {
    super(context, android.R.layout.simple_dropdown_item_1line, list);
    mList = list;
  }

  public List<T> getList() {
    return mList;
  }

  @Override
  public Filter getFilter() {
    return new ComboFilter(mList);
  }
}
