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
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.android.recyclerview.ClickableViewHolder;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.OverviewTaskListitemBinding;
import nl.adaptivity.process.tasks.ExecutableUserTask.TaskState;
import nl.adaptivity.process.ui.task.OverviewTaskCursorAdapter.OverviewTaskCursorViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class OverviewTaskCursorAdapter extends BaseTaskCursorAdapter<OverviewTaskCursorViewHolder> {

  public final class OverviewTaskCursorViewHolder extends ClickableViewHolder {

    public final OverviewTaskListitemBinding binding;

    public OverviewTaskCursorViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(OverviewTaskCursorAdapter.this, inflater.inflate(R.layout.overview_task_listitem, parent, false));
      binding = DataBindingUtil.bind(itemView);
    }
  }

  public OverviewTaskCursorAdapter(final Context context, final Cursor c) {
    super(context, c, false);
  }

  @Override
  public void onBindViewHolder(final OverviewTaskCursorViewHolder viewHolder, final Cursor cursor) {
    super.onBindViewHolder(viewHolder, cursor);
    viewHolder.binding.setSummary(mSummaryColIdx >= 0 ? cursor.getString(mSummaryColIdx) : null);
    viewHolder.binding.setInstanceName(mInstNameColIdx>=0 ? cursor.getString(mInstNameColIdx): null);
    final int drawableId;
    if (mStateColIdx >= 0) {
      final String    s     = cursor.getString(mStateColIdx);
      final TaskState state = TaskState.fromString(s);
      drawableId = state == null ? 0 : state.getDecoratorId();
    } else {
      drawableId = 0;
    }
    viewHolder.binding.setTaskStateDrawable(drawableId);
//      viewHolder.binding.executePendingBindings();
  }

  @Override
  public OverviewTaskCursorViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    return new OverviewTaskCursorViewHolder(mInflater, parent);
  }

}
