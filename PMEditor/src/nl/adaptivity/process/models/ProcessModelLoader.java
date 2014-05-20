package nl.adaptivity.process.models;

import nl.adaptivity.process.processModel.ProcessModel;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.Uri;


public class ProcessModelLoader extends AsyncTaskLoader<ProcessModel<?>> {

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
  public ProcessModel<?> loadInBackground() {
    if (mHandle>=0) {
      return ProcessModelProvider.getProcessModelForHandle(getContext(),mHandle);
    } else {
      return ProcessModelProvider.getProcessModel(getContext(), mUri);
    }
  }

}
