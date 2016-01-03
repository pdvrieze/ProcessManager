package nl.adaptivity.process.ui.task;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.view.LayoutInflater;
import nl.adaptivity.android.util.ClickableCursorAdapter.ClickableViewHolder;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.ui.task.TaskCursorAdapter.TaskCursorViewHolder;


/**
 * Created by pdvrieze on 03/01/16.
 */
public abstract class BaseTaskCursorAdapter<VH extends ClickableViewHolder> extends nl.adaptivity.android.util.SelectableCursorAdapter<VH> {

  protected LayoutInflater mInflater;
  protected int mSummaryColIdx;
  protected int mStateColIdx;

  public BaseTaskCursorAdapter(final Context context, final Cursor cursor, final boolean allowUnselection) {
    super(context, cursor, allowUnselection);
    mInflater = LayoutInflater.from(context);
    setHasStableIds(true);
    updateColIdxs(cursor);

  }

  @CallSuper
  protected void updateColIdxs(Cursor c) {
    if (c == null) {
      mSummaryColIdx = -1;
      mStateColIdx = -1;
    } else {
      mSummaryColIdx = c.getColumnIndex(Tasks.COLUMN_SUMMARY);
      mStateColIdx = c.getColumnIndex(Tasks.COLUMN_STATE);
    }
  }

  @Override
  public final void changeCursor(Cursor cursor) {
    super.changeCursor(cursor);
  }

  @Override
  public final Cursor swapCursor(Cursor newCursor) {
    final Cursor result = super.swapCursor(newCursor);
    updateColIdxs(newCursor);
    return result;
  }
}
