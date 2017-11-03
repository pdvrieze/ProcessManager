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
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import nl.adaptivity.process.editor.android.BuildConfig;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pdvrieze on 28/02/16.
 */
public class SyncManager {

  private static final String TAG = "SyncManager";

  public class SyncStatusObserverData {
    final SyncStatusObserver observer;
    final String             authority;

    public SyncStatusObserverData(final String authority, final SyncStatusObserver syncObserver) {
      this.authority = authority;
      this.observer = syncObserver;
    }
  }

  private static final boolean DISABLEONDEBUG = false;
  private final Account mAccount;
  private String[]      mAuthorities;
  private final List<SyncStatusObserverData> mSyncObservers = new ArrayList<>(2);

  private final SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
    @Override
    public void onStatusChanged(final int which) {
      onInnerSyncStatusChanged(which);
    }
  };
  @Nullable private Object mSyncObserverHandle;

  private void onInnerSyncStatusChanged(final int which) {
    if (mAccount!=null) {
      synchronized (this) {
        for (final String authority : mAuthorities) {
          if (ContentResolver.isSyncActive(mAccount, authority) || ContentResolver.isSyncPending(mAccount, authority)) {
            for (final SyncStatusObserverData observerData : mSyncObservers) {
              if (authority.equals(observerData.authority)) {
                observerData.observer.onStatusChanged(which);
              }
            }
          }
        }
      }
    }
  }

  public SyncManager(final Account account, final String[] authorities) {
    mAccount = account;
    mAuthorities = authorities;
  }

  @NonNull
  public SyncStatusObserverData addOnStatusChangeObserver(final String authority, @NonNull final SyncStatusObserver syncObserver) {
    final SyncStatusObserverData data;
    synchronized (this) {
      if (mSyncObserverHandle ==null && isSyncable(authority)) {
        Log.d(TAG, "TRACE: OverallSyncObserver");
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE | ContentResolver.SYNC_OBSERVER_TYPE_PENDING, mSyncObserver);
      }
      data = new SyncStatusObserverData(authority, syncObserver);
      Log.d(TAG, "TRACE: SyncObserver_"+authority);

      mSyncObservers.add(data);
    }
    return data;
  }

  public void removeOnStatusChangeObserver(@NonNull final SyncStatusObserverData handle) {
    Log.d(TAG, "TRACE: Remove SyncObserver_"+handle.authority);
    synchronized (this) {
      if (mSyncObserverHandle != null && mSyncObservers.remove(handle) && mSyncObservers.isEmpty()) {
        ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
        Log.d(TAG, "TRACE: Remove OverallSyncObserver");
        mSyncObserverHandle = null;
      }
    }
  }

  public boolean isSyncable(final String authority) {
    if (mAccount==null||(DISABLEONDEBUG && BuildConfig.DEBUG && Debug.isDebuggerConnected())) { return false; }
    return ContentResolver.getIsSyncable(mAccount, authority) > 0;
  }

  @NonNull
  public List<String> getActiveSyncTargets() {
    final List<String> result = new ArrayList<>(2);
    synchronized (this) {
      for (final String authority : mAuthorities) {
        if (isSyncable(authority)) {
          result.add(authority);
        }
      }
    }
    return result;
  }

  public Account getAccount() {
    return mAccount;
  }

  public void verifyNoObserversActive() {
    synchronized (this) {
      for (SyncStatusObserverData syncStatusObserverData : mSyncObservers) {
        Log.w(TAG, "Synchronization observer active for " + syncStatusObserverData.authority);
      }
    }
    if (mSyncObserverHandle!=null) {
      Log.w(TAG, "Overall synchronization observer still active", new IllegalStateException());
    }
  }
}
