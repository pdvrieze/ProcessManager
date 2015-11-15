package nl.adaptivity.process.models;

import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.processModel.ProcessModel;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;


public class ProcessModelLoader extends AsyncTaskLoader<ProcessModelHolder> {

  private Uri mUri=null;
  private long mHandle=-1L;

  public ProcessModelLoader(Context context, long handle) {
    super(context);
    mHandle = handle;
    onContentChanged();
  }

  public ProcessModelLoader(Context context, Uri uri) {
    super(context);
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
    final ProcessModel<?> processModel;
    if (handle!=null) {
      processModel = ProcessModelProvider.getProcessModelForHandle(getContext(),mHandle);
    } else {
      processModel = ProcessModelProvider.getProcessModel(getContext(), mUri);
      long id = ContentUris.parseId(mUri);
      Cursor handleCursor = getContext().getContentResolver().query(ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id), new String[] {ProcessModels.COLUMN_HANDLE}, null, null, null);
      try {
        handleCursor.moveToFirst();
        if (!handleCursor.isNull(0)) {
          handle = Long.valueOf(handleCursor.getLong(0));
        }
      } finally {
        handleCursor.close();
      }
    }
    return new ProcessModelHolder(processModel, handle);
  }

}
