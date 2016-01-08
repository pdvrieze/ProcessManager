package nl.adaptivity.android.util;

import android.os.AsyncTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


/**
 * Created by pdvrieze on 15/12/15.
 */
public class AsyncCallableTask<T, C extends Callable<T>> extends AsyncTask<C, Void, Future<T>> {

  private FutureTask<T> mFuture;
  private C mCallable;

  @Override
  protected Future<T> doInBackground(final C... params) {
    mCallable = params[0];
    mFuture = new FutureTask<T>(mCallable);
    mFuture.run(); // Run here
    return mFuture;
  }

  protected C getCallable() {
    return mCallable;
  }

}
