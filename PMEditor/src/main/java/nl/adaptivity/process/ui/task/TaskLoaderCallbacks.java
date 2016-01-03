package nl.adaptivity.process.ui.task;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import nl.adaptivity.process.ui.main.ListCursorLoaderCallbacks;


/**
 * Created by pdvrieze on 28/12/15.
 */
public class TaskLoaderCallbacks extends ListCursorLoaderCallbacks<BaseTaskCursorAdapter> {

  public TaskLoaderCallbacks(final Context context, final BaseTaskCursorAdapter adapter) {
    super(context, adapter);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(mContext, TaskProvider.Tasks.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, Tasks.COLUMN_SUMMARY, Tasks.COLUMN_STATE}, Tasks.COLUMN_STATE + "!='Complete'", null, null);
  }

}
