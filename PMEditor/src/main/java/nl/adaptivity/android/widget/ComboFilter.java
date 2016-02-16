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

package nl.adaptivity.android.widget;

import android.widget.Filter;

import java.util.List;


/**
 * Created by pdvrieze on 16/02/16.
 */
public class ComboFilter<T> extends Filter {

  private List<T> mList;

  public ComboFilter(final List<T> list) {
    mList = list;
  }

  @Override
  protected FilterResults performFiltering(final CharSequence constraint) {
    final FilterResults result = new FilterResults();
    result.count = mList.size();
    result.values = mList;
    return result;
  }

  public List<T> getList() {
    return mList;
  }

  @Override
  protected void publishResults(final CharSequence constraint, final FilterResults results) {
    // We don't change the results.
  }

}
