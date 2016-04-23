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
import nl.adaptivity.sync.RemoteXmlSyncAdapter;


/**
 * Created by pdvrieze on 28/12/15.
 */
public class TaskLoaderCallbacks extends ListCursorLoaderCallbacks<BaseTaskCursorAdapter> {

  public TaskLoaderCallbacks(final Context context, final BaseTaskCursorAdapter adapter) {
    super(context, adapter);
  }

  @Override
  public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
    return new CursorLoader(mContext, TaskProvider.Tasks.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, Tasks.COLUMN_SUMMARY, Tasks.COLUMN_STATE}, Tasks.COLUMN_STATE + "!='Complete' AND " + Tasks.COLUMN_STATE + "!=" + RemoteXmlSyncAdapter.SYNC_NEWDETAILSPENDING, null, null);
  }

}
