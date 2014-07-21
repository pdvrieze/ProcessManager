package nl.adaptivity.process.tasks.data;

import nl.adaptivity.process.tasks.UserTask;
import android.content.Context;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;


public class TaskLoader extends AsyncTaskLoader<UserTask> {

  private Uri mUri=null;
  private long mHandle=-1L;

  public TaskLoader(Context pContext, long pHandle) {
    super(pContext);
    mHandle = pHandle;
    onContentChanged();
  }

  public TaskLoader(Context pContext, Uri pUri) {
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
  public UserTask loadInBackground() {
    if (mHandle>=0) {
      return TaskProvider.getTaskForHandle(getContext(),mHandle);
    } else {
      return TaskProvider.getTask(getContext(), mUri);
    }
  }

}
