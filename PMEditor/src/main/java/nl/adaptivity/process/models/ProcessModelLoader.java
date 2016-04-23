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

package nl.adaptivity.process.models;

import android.content.ContentResolver;
import net.devrieze.util.Tupple;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;


public class ProcessModelLoader extends AsyncTaskLoader<ProcessModelHolder> {

  private Uri mUri=null;
  private long mHandle=-1L;
  private final ForceLoadContentObserver mObserver;

  public ProcessModelLoader(final Context context, final long handle) {
    super(context);
    mObserver = new ForceLoadContentObserver();
    mHandle = handle;
    onContentChanged();
  }

  public ProcessModelLoader(final Context context, final Uri uri) {
    super(context);
    mObserver = new ForceLoadContentObserver();
    mUri = uri;
    onContentChanged();
  }


  @Override
  protected void onStartLoading() {
    super.onStartLoading();
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  public ProcessModelHolder loadInBackground() {
    Long handle = mHandle>=0 ? Long.valueOf(mHandle) : null;
    final DrawableProcessModel processModel;
    final long id;
    if (handle!=null) {
      final Tupple<DrawableProcessModel, Long> tupple = ProcessModelProvider.getProcessModelForHandle(getContext(), mHandle);
      processModel = tupple.getElem1();
      id = tupple.getElem2();
    } else {
      processModel = ProcessModelProvider.getProcessModel(getContext(), mUri);
      id = ContentUris.parseId(mUri);
      final Cursor handleCursor = getContext().getContentResolver().query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id), new String[] {ProcessModels.COLUMN_HANDLE}, null, null, null);
      try {
        handleCursor.moveToFirst();
        if (!handleCursor.isNull(0)) {
          handle = Long.valueOf(handleCursor.getLong(0));
        }
      } finally {
        handleCursor.close();
      }
    }
    final Uri             updateUri       = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id);
    final ContentResolver contentResolver = getContext().getContentResolver();
    contentResolver.registerContentObserver(updateUri, false, mObserver);
    return new ProcessModelHolder(processModel, handle);
  }

  @Override
  protected void onAbandon() {
    getContext().getContentResolver().unregisterContentObserver(mObserver);
  }

}
