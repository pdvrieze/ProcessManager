package nl.adaptivity.process.models;

import android.content.ContentResolver;
import android.support.v4.content.Loader;
import net.devrieze.util.Tupple;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.processModel.ProcessModel;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;
import nl.adaptivity.process.tasks.UserTask;


public class ProcessModelLoader extends AsyncTaskLoader<ProcessModelHolder> {

  private Uri mUri=null;
  private long mHandle=-1L;
  private ForceLoadContentObserver mObserver;

  public ProcessModelLoader(Context context, long handle) {
    super(context);
    mObserver = new ForceLoadContentObserver();
    mHandle = handle;
    onContentChanged();
  }

  public ProcessModelLoader(Context context, Uri uri) {
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
    final ProcessModel<?, ?> processModel;
    final long id;
    if (handle!=null) {
      final Tupple<ProcessModel<?, ?>, Long> tupple = ProcessModelProvider.getProcessModelForHandle(getContext(), mHandle);
      processModel = tupple.getElem1();
      id = tupple.getElem2();
    } else {
      processModel = ProcessModelProvider.getProcessModel(getContext(), mUri);
      id = ContentUris.parseId(mUri);
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
    Uri updateUri = ContentUris.withAppendedId(ProcessModels.CONTENT_ID_URI_BASE, id);
    ContentResolver contentResolver = getContext().getContentResolver();
    contentResolver.registerContentObserver(updateUri, false, mObserver);
    return new ProcessModelHolder(processModel, handle);
  }

}
