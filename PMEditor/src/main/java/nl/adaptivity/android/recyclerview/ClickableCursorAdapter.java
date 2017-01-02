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

import android.content.Context;
import android.database.Cursor;


/**
 * Created by pdvrieze on 28/12/15.
 */
public abstract class ClickableCursorAdapter<VH extends ClickableViewHolder> extends CursorRecyclerViewAdapter<VH> implements ClickableAdapter<VH> {

  private OnItemClickListener<? super VH> mItemClickListener;

  public ClickableCursorAdapter(final Context context, final Cursor cursor) {super(context, cursor);}

  @Override
  public final void doClickView(final VH viewHolder) {
    if (mItemClickListener==null || (! mItemClickListener.onClickItem(this, viewHolder))) {
      onClickView(viewHolder);
    }
  }

  public void onClickView(final VH viewHolder) {}

  @Override
  public OnItemClickListener getOnItemClickListener() {
    return mItemClickListener;
  }

  @Override
  public void setOnItemClickListener(final OnItemClickListener<? super VH> itemClickListener) {
    mItemClickListener = itemClickListener;
  }
}
