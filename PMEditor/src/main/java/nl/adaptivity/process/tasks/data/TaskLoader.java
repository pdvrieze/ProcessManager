package nl.adaptivity.process.tasks.data;

import nl.adaptivity.process.tasks.UserTask;
import nl.adaptivity.process.tasks.data.TaskProvider.Tasks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;


public class TaskLoader extends AsyncTaskLoader<UserTask> {

  private Uri mUri=null;
  private long mHandle=-1L;
  private Loader<UserTask>.ForceLoadContentObserver mObserver;

  public TaskLoader(Context context, long handle) {
    super(context);
    mObserver = new ForceLoadContentObserver();
    mHandle = handle;
    onContentChanged();
  }

  public TaskLoader(Context context, Uri uri) {
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
  public UserTask loadInBackground() {
    UserTask task;
    final ContentResolver contentResolver = getContext().getContentResolver();
    if (mHandle>=0) {
      Cursor idresult = contentResolver.query(Tasks.CONTENT_URI, new String[] { BaseColumns._ID }, Tasks.COLUMN_HANDLE+" = ?", new String[] { Long.toString(mHandle)} , null);
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

}
