package nl.adaptivity.process.ui.task;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.android.recyclerview.ClickableCursorAdapter;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.TaskListitemBinding;
import nl.adaptivity.process.tasks.UserTask.TaskState;
import nl.adaptivity.process.ui.task.TaskCursorAdapter.TaskCursorViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class TaskCursorAdapter extends BaseTaskCursorAdapter<TaskCursorViewHolder> {

  public final class TaskCursorViewHolder extends ClickableCursorAdapter<TaskCursorViewHolder>.ClickableViewHolder {

    public final TaskListitemBinding binding;

    public TaskCursorViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(inflater.inflate(R.layout.task_listitem, parent, false));
      binding = DataBindingUtil.bind(itemView);
    }
  }

  public TaskCursorAdapter(Context context, Cursor c) {
    super(context, c, false);
  }

  @Override
  public void onBindViewHolder(final TaskCursorViewHolder viewHolder, final Cursor cursor) {
    super.onBindViewHolder(viewHolder, cursor);
    viewHolder.binding.setSummary(mSummaryColIdx >= 0 ? cursor.getString(mSummaryColIdx) : null);
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
  public TaskCursorViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    return new TaskCursorViewHolder(mInflater, parent);
  }

}
