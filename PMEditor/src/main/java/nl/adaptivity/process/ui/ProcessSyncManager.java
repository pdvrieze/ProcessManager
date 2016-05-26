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

package nl.adaptivity.process.ui;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.preference.PreferenceManager;
import nl.adaptivity.process.data.ProviderHelper;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessSyncAdapter;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.tasks.data.TaskSyncAdapter;
import nl.adaptivity.process.ui.main.SettingsActivity;
import nl.adaptivity.sync.DirectSyncTask;
import nl.adaptivity.sync.SyncManager;


/**
 * Created by pdvrieze on 24/04/16.
 */
public class ProcessSyncManager extends SyncManager {
  private static final String[] AUTHORITIES     = new String[]{ProcessModelProvider.AUTHORITY, TaskProvider.AUTHORITY};
  public static final long      DEFAULT_MIN_AGE = 10000; // Don't refresh if less then 10 seconds ago
  private final Context context;

  private boolean syncingProcesses = false;
  private boolean syncingTasks     = false;
  private long    lastProcessSync  = 0;
  private long    lastTaskSync     = 0;

  private final Runnable      ONCOMPLETEPROCESSES = new Runnable() {
    @Override
    public void run() {
      synchronized (ProcessSyncManager.this) {
        syncingProcesses = false;
      }
    }
  };
  private final Runnable      ONCOMPLETETASKS = new Runnable() {
    @Override
    public void run() {
      synchronized (ProcessSyncManager.this) {
        syncingTasks = false;
      }
    }
  };

  public ProcessSyncManager(final Context context, final Account account) {
    super(account, AUTHORITIES);
    this.context = context;
  }

  public static boolean isLocalsync(final Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_SYNC_LOCAL, false);
  }

  public boolean isLocalsync() {
    return isLocalsync(context);
  }

  public boolean isProcessModelSyncActive() {
    return ContentResolver.isSyncActive(getAccount(), ProcessModelProvider.AUTHORITY);
  }

  public boolean isProcessModelSyncPending() {
    return ContentResolver.isSyncPending(getAccount(), ProcessModelProvider.AUTHORITY);
  }

  public boolean isTaskSyncActive() {
    return ContentResolver.isSyncActive(getAccount(), TaskProvider.AUTHORITY);
  }


  public boolean isTaskSyncPending() {
    return ContentResolver.isSyncPending(getAccount(), TaskProvider.AUTHORITY);
  }

  public void requestSyncProcessModelList(final boolean expedited, final long minAge) {
    long now = System.currentTimeMillis();
    if (getAccount()!=null && (now-lastProcessSync)>minAge) {
      lastProcessSync=now;
      if (!isLocalsync() && isSyncable(ProcessModelProvider.AUTHORITY)) {
        ProviderHelper.requestSync(getAccount(), ProcessModelProvider.AUTHORITY, expedited);
      } else {
        ContentResolver       contentResolver = context.getContentResolver();
        ContentProviderClient providerClient  = contentResolver.acquireContentProviderClient(ProcessModelProvider.AUTHORITY);
        DirectSyncTask        syncTask        = new DirectSyncTask(new ProcessSyncAdapter(context), getAccount(), providerClient, ONCOMPLETEPROCESSES);
        synchronized (this) {
          if (!syncingProcesses) {
            syncingProcesses = true;
            syncTask.execute(ProcessModelProvider.AUTHORITY);
          }
        }
      }
    }
  }

  public void requestSyncTaskList(final boolean expedited, final long minAge) {
    long now = System.currentTimeMillis();
    if (getAccount()!=null && (now-lastTaskSync)>minAge) {
      lastTaskSync=now;
      if (!isLocalsync() && isSyncable(TaskProvider.AUTHORITY)) {
        ProviderHelper.requestSync(getAccount(), TaskProvider.AUTHORITY, expedited);
      } else {
        ContentResolver       contentResolver = context.getContentResolver();
        ContentProviderClient providerClient  = contentResolver.acquireContentProviderClient(TaskProvider.AUTHORITY);
        DirectSyncTask        syncTask        = new DirectSyncTask(new TaskSyncAdapter(context), getAccount(), providerClient, ONCOMPLETETASKS);
        synchronized (this) {
          if (!syncingTasks) {
            syncingTasks = true;
            syncTask.execute(TaskProvider.AUTHORITY);
          }
        }
      }
    }
  }

}
