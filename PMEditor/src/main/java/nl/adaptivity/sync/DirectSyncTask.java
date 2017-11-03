/*
 * Copyright (c) 2017.
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

package nl.adaptivity.sync;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import nl.adaptivity.process.tasks.data.TaskProvider;


/**
 * Created by pdvrieze on 09/05/16.
 */
public class DirectSyncTask extends AsyncTask<String, Void, Void>{

  private final LocalSyncAdapter mSyncAdapter;
  private final Context mContext;
  private final String mAuthority;
  private Account mAccount;

  private final Runnable mOnComplete;

  public DirectSyncTask(final LocalSyncAdapter syncAdapter, Account account, final Context context, final String authority, final Runnable onComplete) {
    mSyncAdapter = syncAdapter;
    mAccount = account;
    mContext = context;
    mAuthority = authority;
    mOnComplete = onComplete;
  }

  @Nullable
  @Override
  protected Void doInBackground(final String... params) {
    final ContentProviderClient providerClient = mContext.getContentResolver().acquireContentProviderClient(TaskProvider.AUTHORITY);
    try {
      SyncResult syncResult = new SyncResult();
      mSyncAdapter.onPerformLocalSync(mAccount, new Bundle(0), params[0], providerClient, syncResult);
    } finally {
      providerClient.release();
    }
    return null;
  }

  @Override
  protected void onPostExecute(final Void aVoid) {
    if (mOnComplete!=null) {
      mOnComplete.run();
    }
  }

  @Override
  protected void onCancelled(final Void aVoid) {
    if (mOnComplete!=null) {
      mOnComplete.run();
    }
  }
}
