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

package nl.adaptivity.process.tasks.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import nl.adaptivity.process.tasks.ExecutableUserTask;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;


public class TaskLoader extends AsyncTaskLoader<ExecutableUserTask> {

  private Uri mUri=null;
  private long mHandle=-1L;
  private ExecutableUserTask mData = null;
  private final Loader<ExecutableUserTask>.ForceLoadContentObserver mObserver;

  public TaskLoader(final Context context, final long handle) {
    super(context);
    mObserver = new ForceLoadContentObserver();
    mHandle = handle;
    onContentChanged();
  }

  public TaskLoader(final Context context, final Uri uri) {
    super(context);
    mObserver = new ForceLoadContentObserver();
    mUri = uri;
    onContentChanged();
  }


  @Override
  protected void onStartLoading() {
    super.onStartLoading();
    if (mData==null || takeContentChanged()) {
      forceLoad();
    } else {
      deliverResult(mData);
    }
  }

  @Override
  public ExecutableUserTask loadInBackground() {
    final ExecutableUserTask task;
    final ContentResolver    contentResolver = getContext().getContentResolver();
    if (mHandle>=0) {
      final Cursor idresult = contentResolver.query(Tasks.CONTENT_URI, new String[] {BaseColumns._ID }, Tasks.COLUMN_HANDLE + " = ?", new String[] {Long.toString(mHandle)} , null);
      try {
        if (! idresult.moveToFirst()) { return null; }
        mUri = ContentUris.withAppendedId(Tasks.CONTENT_URI, idresult.getLong(0));
        mHandle = -1;
      } finally {
        idresult.close();
      }
    }
    task = TaskProvider.getTask(getContext(), mUri);
    contentResolver.registerContentObserver(mUri, false, mObserver);
    return task;
  }

  @Override
  protected void onReset() {
    getContext().getContentResolver().unregisterContentObserver(mObserver);
  }
}
