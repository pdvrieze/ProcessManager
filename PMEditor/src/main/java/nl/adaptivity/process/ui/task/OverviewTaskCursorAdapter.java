package nl.adaptivity.process.ui.task;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.android.recyclerview.ClickableCursorAdapter;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.OverviewTaskListitemBinding;
import nl.adaptivity.process.tasks.UserTask.TaskState;
import nl.adaptivity.process.ui.task.OverviewTaskCursorAdapter.OverviewTaskCursorViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class OverviewTaskCursorAdapter extends BaseTaskCursorAdapter<OverviewTaskCursorViewHolder> {

  public final class OverviewTaskCursorViewHolder extends ClickableCursorAdapter<OverviewTaskCursorViewHolder>.ClickableViewHolder {

    public final OverviewTaskListitemBinding binding;

    public OverviewTaskCursorViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(inflater.inflate(R.layout.overview_task_listitem, parent, false));
      binding = DataBindingUtil.bind(itemView);
    }
  }

  public OverviewTaskCursorAdapter(Context context, Cursor c) {
    super(context, c, false);
  }

  @Override
  public void onBindViewHolder(final OverviewTaskCursorViewHolder viewHolder, final Cursor cursor) {
    super.onBindViewHolder(viewHolder, cursor);
    viewHolder.binding.setSummary(mSummaryColIdx >= 0 ? cursor.getString(mSummaryColIdx) : null);
    viewHolder.binding.setInstanceName(mInstNameColIdx>=0 ? cursor.getString(mInstNameColIdx): null);
    final int drawableId;
    if (mStateColIdx >= 0) {
      String s = cursor.getString(mStateColIdx);
      TaskState state = TaskState.fromString(s);
      drawableId = state == null ? -1 : state.getDecoratorId();
    } else {
      drawableId = -1;
    }
    viewHolder.binding.setTaskStateDrawable(drawableId);
//      viewHolder.binding.executePendingBindings();
  }

  @Override
  public OverviewTaskCursorViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    return new OverviewTaskCursorViewHolder(mInflater, parent);
  }

}
