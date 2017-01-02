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

package nl.adaptivity.android.recyclerview;


/**
 * Created by pdvrieze on 04/01/16.
 */
public interface SelectableAdapter<VH extends ClickableViewHolder> extends ClickableAdapter<VH> {

  interface OnSelectionListener<VH extends ClickableViewHolder> {

    void onSelectionChanged(SelectableAdapter<VH> adapter);
  }

  long getSelectedId();

  int getSelectedPos();

  boolean isAllowUnselection();

  OnSelectionListener getOnSelectionListener();

  boolean isSelectionEnabled();

  void setSelection(int position);

  void setSelectedItem(long itemId);

  void setOnSelectionListener(OnSelectionListener onSelectionListener);

  void setSelectionEnabled(boolean enabled);
}
