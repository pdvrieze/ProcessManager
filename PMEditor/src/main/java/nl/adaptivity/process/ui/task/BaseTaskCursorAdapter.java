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

package nl.adaptivity.process.ui.task;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.view.LayoutInflater;
import nl.adaptivity.android.recyclerview.ClickableViewHolder;
import nl.adaptivity.android.recyclerview.SelectableCursorAdapter;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;


/**
 * Created by pdvrieze on 03/01/16.
 */
public abstract class BaseTaskCursorAdapter<VH extends ClickableViewHolder> extends SelectableCursorAdapter<VH> {

  protected final LayoutInflater mInflater;
  protected       int            mSummaryColIdx;
  protected       int            mStateColIdx;
  protected       int            mInstNameColIdx;

  public BaseTaskCursorAdapter(final Context context, final Cursor cursor, final boolean allowUnselection) {
    super(context, cursor, allowUnselection);
    mInflater = LayoutInflater.from(context);
    setHasStableIds(true);
    updateColIdxs(cursor);

  }

  @CallSuper
  protected void updateColIdxs(final Cursor c) {
    if (c == null) {
      mSummaryColIdx = -1;
      mStateColIdx = -1;
      mInstNameColIdx = -1;
    } else {
      mSummaryColIdx = c.getColumnIndex(Tasks.COLUMN_SUMMARY);
      mStateColIdx = c.getColumnIndex(Tasks.COLUMN_STATE);
      mInstNameColIdx = c.getColumnIndex(Tasks.COLUMN_INSTANCENAME);
    }
  }

  @Override
  public final Cursor swapCursor(final Cursor newCursor) {
    final Cursor result = super.swapCursor(newCursor);
    updateColIdxs(newCursor);
    return result;
  }
}
