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
