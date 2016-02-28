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

package nl.adaptivity.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.os.Debug;
import android.support.annotation.NonNull;
import nl.adaptivity.process.data.ProviderHelper;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.tasks.data.TaskProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pdvrieze on 28/02/16.
 */
public class SyncManager {

  public class SyncStatusObserverData {
    SyncStatusObserver observer;
    String authority;

    public SyncStatusObserverData(final String authority, final SyncStatusObserver syncObserver) {
      this.authority = authority;
      this.observer = syncObserver;
    }
  }

  private static final boolean DISABLEONDEBUG = true;
  private static final String[] AUTHORITIES = new String[]{ProcessModelProvider.AUTHORITY, TaskProvider.AUTHORITY};
  private final Account mAccount;
  private List<SyncStatusObserverData> mSyncObservers = new ArrayList<>(2);

  private final SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
    @Override
    public void onStatusChanged(final int which) {
      onInnerSyncStatusChanged(which);
    }
  };
  private Object mSyncObserverHandle;

  private void onInnerSyncStatusChanged(final int which) {
    if (mAccount!=null) {
      for (String authority: AUTHORITIES) {
        if (ContentResolver.isSyncActive(mAccount, authority) || ContentResolver.isSyncPending(mAccount, authority)) {
          for (SyncStatusObserverData observerData : mSyncObservers) {
            if (authority.equals(observerData.authority)) {
              observerData.observer.onStatusChanged(which);
            }
          }
        }
      }
    }
  }

  public SyncManager(Account account) {
    mAccount = account;
  }

  public SyncStatusObserverData addOnStatusChangeObserver(final String authority, @NonNull final SyncStatusObserver syncObserver) {
    SyncStatusObserverData data;
    if (mSyncObserverHandle ==null && isSyncable(authority)) {
      mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE | ContentResolver.SYNC_OBSERVER_TYPE_PENDING, mSyncObserver);
    }
    data = new SyncStatusObserverData(authority, syncObserver);
    mSyncObservers.add(data);
    return data;
  }

  public void removeOnStatusChangeObserver(SyncStatusObserverData handle) {
    if (mSyncObservers.remove(handle) && mSyncObservers.isEmpty()) {
      ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
      mSyncObserverHandle = null;
    }
  }

  public boolean isSyncable(String authority) {
    if (mAccount==null||(DISABLEONDEBUG && BuildConfig.DEBUG && Debug.isDebuggerConnected())) { return false; }
    return ContentResolver.getIsSyncable(mAccount, authority) > 0;
  }

  public List<String> getActiveSyncTargets() {
    List<String> result = new ArrayList<>(2);
    for (String authority: AUTHORITIES) {
      if (isSyncable(authority)) {
        result.add(authority);
      }
    }
    return result;
  }

  public boolean isProcessModelSyncActive() {
    return ContentResolver.isSyncActive(mAccount, ProcessModelProvider.AUTHORITY);
  }

  public boolean isProcessModelSyncPending() {
    return ContentResolver.isSyncPending(mAccount, ProcessModelProvider.AUTHORITY);
  }

  public boolean isTaskSyncActive() {
    return ContentResolver.isSyncActive(mAccount, TaskProvider.AUTHORITY);
  }


  public boolean isTaskSyncPending() {
    return ContentResolver.isSyncPending(mAccount, TaskProvider.AUTHORITY);
  }

  public void requestSyncProcessModelList(final boolean expedited) {
    if (isSyncable(ProcessModelProvider.AUTHORITY)) {
      ProviderHelper.requestSync(mAccount, ProcessModelProvider.AUTHORITY, expedited);
    }
  }

  public void requestSyncTaskList(final boolean expedited) {
    if (isSyncable(TaskProvider.AUTHORITY)) {
      ProviderHelper.requestSync(mAccount, TaskProvider.AUTHORITY, expedited);
    }
  }

}
