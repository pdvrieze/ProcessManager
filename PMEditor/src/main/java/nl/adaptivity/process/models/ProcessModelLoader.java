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

package nl.adaptivity.process.models;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import nl.adaptivity.process.diagram.RootDrawableProcessModel;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;


public class ProcessModelLoader extends AsyncTaskLoader<ProcessModelHolder> {

  private Uri mUri=null;
  private long mHandle=-1L;
  private final ForceLoadContentObserver mObserver;
  private ProcessModelHolder mData = null;

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
    if (mData==null || takeContentChanged()) {
      forceLoad();
    } else {
      deliverResult(mData);
    }
  }

  @Override
  public ProcessModelHolder loadInBackground() {
    Long handle = mHandle>=0 ? Long.valueOf(mHandle) : null;
    final RootDrawableProcessModel.Builder processModel;
    final long id;
    final ProcessModelHolder modelHolder;
    if (handle!=null) {
      modelHolder = ProcessModelProvider.getProcessModelForHandle(getContext(), mHandle);
    } else {
      processModel = ProcessModelProvider.getProcessModel(getContext(), mUri);
      id = ContentUris.parseId(mUri);
      final Cursor handleCursor = getContext().getContentResolver()
                                              .query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id),
                                                     new String[] {ProcessModels.COLUMN_HANDLE,
                                                                   XmlBaseColumns.COLUMN_SYNCSTATE}, null, null, null);
      boolean publicationPending;
      try {
        if (handleCursor.moveToFirst()) {
          handle = handleCursor.isNull(0)? null : Long.valueOf(handleCursor.getLong(0));
          publicationPending = handleCursor.getInt(1) == RemoteXmlSyncAdapter.SYNC_PUBLISH_TO_SERVER;
          modelHolder =new ProcessModelHolder(processModel, handle==null ? -1L :handle, id, publicationPending);
        } else {
          modelHolder=null;
        }
      } finally {
        handleCursor.close();
      }
    }
    if (modelHolder!=null) {
      final Uri             updateUri       = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, modelHolder.getId());

      getContext().getContentResolver().registerContentObserver(updateUri, false, mObserver);
    }
    return modelHolder;
  }

  @Override
  public void deliverResult(final ProcessModelHolder data) {
    mData = data;
    super.deliverResult(data);
  }

  @Override
  protected void onAbandon() {
    getContext().getContentResolver().unregisterContentObserver(mObserver);
  }

}
