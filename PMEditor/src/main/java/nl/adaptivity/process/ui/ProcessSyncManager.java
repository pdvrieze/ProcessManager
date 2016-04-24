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
import android.content.ContentResolver;
import nl.adaptivity.process.data.ProviderHelper;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.sync.SyncManager;


/**
 * Created by pdvrieze on 24/04/16.
 */
public class ProcessSyncManager extends SyncManager {
  private static final String[] AUTHORITIES = new String[]{ProcessModelProvider.AUTHORITY, TaskProvider.AUTHORITY};

  public ProcessSyncManager(final Account account) {
    super(account, AUTHORITIES);
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

  public void requestSyncProcessModelList(final boolean expedited) {
    if (isSyncable(ProcessModelProvider.AUTHORITY)) {
      ProviderHelper.requestSync(getAccount(), ProcessModelProvider.AUTHORITY, expedited);
    }
  }

  public void requestSyncTaskList(final boolean expedited) {
    if (isSyncable(TaskProvider.AUTHORITY)) {
      ProviderHelper.requestSync(getAccount(), TaskProvider.AUTHORITY, expedited);
    }
  }

}
