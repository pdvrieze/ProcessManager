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

package nl.adaptivity.android.recyclerview;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import nl.adaptivity.android.recyclerview.ClickableCursorAdapter.ClickableViewHolder;


/**
 * Class that allows selection state to be maintained in a recyclerview.
 */
public abstract class SelectableCursorAdapter<VH extends ClickableViewHolder> extends ClickableCursorAdapter<VH> implements nl.adaptivity.android.recyclerview.SelectableAdapter {

  private int mSelectionPos = RecyclerView.NO_POSITION;
  private long mSelectionId = RecyclerView.NO_ID;
  private final boolean mAllowUnselection;
  private boolean mSelectionEnabled = true;
  private OnSelectionListener mOnSelectionListener;

  public SelectableCursorAdapter(final Context context, final Cursor cursor, final boolean allowUnselection) {super(context, cursor);
    mAllowUnselection = allowUnselection;
  }

  @Override
  public void onClickView(final ViewHolder viewHolder) {
    if (mSelectionEnabled) {
      setSelection(viewHolder.getAdapterPosition());
    }
  }

  @CallSuper
  public void onBindViewHolder(final VH viewHolder, final Cursor cursor) {
//    viewHolder.itemView.setSelected(viewHolder.getAdapterPosition()==mSelection);
    if (hasStableIds()) {
      viewHolder.itemView.setActivated(viewHolder.getItemId() == mSelectionId);
    } else {
      viewHolder.itemView.setActivated(viewHolder.getAdapterPosition() == mSelectionPos);
    }
  }

  @Override
  public long getSelectedId() {
    return mSelectionId;
  }

  @Override
  public int getSelectedPos() {
    return mSelectionPos;
  }

  @Override
  public void setSelection(final int position) {
    final long itemId = getItemId(position);
    setSelection(position, itemId);
  }

  /**
   * Set the selection to the given ItemId. Note that this may loop through the entire cursor
   * to find the id.
   * @param itemId
   */
  @Override
  public void setSelectedItem(final long itemId) {
    setSelection(getItemPos(itemId), itemId);
  }

  private void setSelection(final int position, final long itemId) {
    if (mSelectionPos != RecyclerView.NO_POSITION) {
      notifyItemChanged(mSelectionPos);
    }
    if (position!=RecyclerView.NO_POSITION && mSelectionPos == position) {
      if (isAllowUnselection()) {
        // unselect
        mSelectionPos = RecyclerView.NO_POSITION;
        mSelectionId = RecyclerView.NO_ID;
        if (mOnSelectionListener != null) { mOnSelectionListener.onSelectionChanged(this); }
      }
    } else {
      mSelectionPos = position;
      mSelectionId = itemId;
      if (mOnSelectionListener != null) { mOnSelectionListener.onSelectionChanged(this); }
      if (position!=RecyclerView.NO_POSITION) {
        notifyItemChanged(position);
      }
    }
  }

  @Override
  public Cursor swapCursor(final Cursor newCursor) {
    Cursor oldCursor = super.swapCursor(newCursor);
    if (hasStableIds() && newCursor!=null && mSelectionId!=RecyclerView.NO_ID) {
      setSelectedItem(mSelectionId);
    }
    return oldCursor;
  }

  @Override
  public boolean isAllowUnselection() {
    return mAllowUnselection;
  }

  @Override
  public OnSelectionListener getOnSelectionListener() {
    return mOnSelectionListener;
  }

  @Override
  public void setOnSelectionListener(final OnSelectionListener onSelectionListener) {
    mOnSelectionListener = onSelectionListener;
  }

  @Override
  public boolean isSelectionEnabled() {
    return mSelectionEnabled;
  }

  @Override
  public void setSelectionEnabled(final boolean enabled) {
    mSelectionEnabled = enabled;
  }
}
