package nl.adaptivity.process.ui.task;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.android.util.ClickableCursorAdapter;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.TaskListitemBinding;
import nl.adaptivity.process.tasks.UserTask.TaskState;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.ui.task.TaskCursorAdapter.TaskCursorViewHolder;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class TaskCursorAdapter extends nl.adaptivity.android.util.SelectableCursorAdapter<TaskCursorViewHolder> {

  public final class TaskCursorViewHolder extends ClickableCursorAdapter.ClickableViewHolder {

    public final TaskListitemBinding binding;

    public TaskCursorViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(inflater.inflate(R.layout.task_listitem, parent, false));
      binding = DataBindingUtil.bind(itemView);
    }
  }

  private LayoutInflater mInflater;
  private int mSummaryColIdx;
  private int mStateColIdx;

  public TaskCursorAdapter(Context context, Cursor c) {
    super(context, c, false);
    setHasStableIds(true);
    mInflater = LayoutInflater.from(context);
    updateColIdxs(c);
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

  private void updateColIdxs(Cursor c) {
    if (c == null) {
      mSummaryColIdx = -1;
      mStateColIdx = -1;
    } else {
      mSummaryColIdx = c.getColumnIndex(Tasks.COLUMN_SUMMARY);
      mStateColIdx = c.getColumnIndex(Tasks.COLUMN_STATE);
    }
  }

  @Override
  public void changeCursor(Cursor cursor) {
    super.changeCursor(cursor);
  }

  @Override
  public Cursor swapCursor(Cursor newCursor) {
    final Cursor result = super.swapCursor(newCursor);
    updateColIdxs(newCursor);
    return result;
  }
}
