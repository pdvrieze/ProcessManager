package nl.adaptivity.process.models;

import nl.adaptivity.process.models.ProcessModelLoader.ProcessModelHolder;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.processModel.ProcessModel;
import android.content.AsyncTaskLoader;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;


public class ProcessModelLoader extends AsyncTaskLoader<ProcessModelHolder> {

  public static class ProcessModelHolder {
    public ProcessModelHolder(ProcessModel<?> pModel, Long pHandle) {
      model = pModel;
      handle = pHandle;
    }
    public final ProcessModel<?> model;
    public final Long handle;
  }

  private Uri mUri=null;
  private long mHandle=-1L;

  public ProcessModelLoader(Context pContext, long pHandle) {
    super(pContext);
    mHandle = pHandle;
    onContentChanged();
  }

  public ProcessModelLoader(Context pContext, Uri pUri) {
    super(pContext);
    mUri = pUri;
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
