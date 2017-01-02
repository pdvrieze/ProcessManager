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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.main;

import android.content.SyncStatusObserver;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.ActivityOverviewBinding;
import nl.adaptivity.process.editor.android.databinding.ActivityOverviewDebugBinding;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.sync.SyncManager.SyncStatusObserverData;

import java.lang.ref.WeakReference;


/**
 * A version of overview activity that adds additional debug status.
 */
public class DebugOverviewActivity extends OverviewActivity implements OnClickListener {


  private static class MySyncObserver implements SyncStatusObserver {

    private final WeakReference<DebugOverviewActivity> mOwner;

    public MySyncObserver(final DebugOverviewActivity owner) {
      mOwner = new WeakReference<>(owner);
    }

    @Override
    public void onStatusChanged(final int which) {
      if (mOwner.isEnqueued()) { mOwner.clear(); return; }
      DebugOverviewActivity owner = mOwner.get();
      if (owner!=null) {
        owner.updateSyncStatus();
      }
    }
  }

  private static class MyRefreshHandler extends Handler {

    private final WeakReference<DebugOverviewActivity> mOwner;
    private boolean active = false;

    public MyRefreshHandler(final DebugOverviewActivity owner) {
      super(Looper.getMainLooper());
      mOwner = new WeakReference<>(owner);
    }

    public void resume() {
      active = true;
      sendEmptyMessageDelayed(1, 5000);
    }

    public void pause() {
      active = false;
      removeMessages(1);
    }

    @Override
    public void handleMessage(final Message msg) {
      if (mOwner.isEnqueued()) { mOwner.clear(); return; }
      DebugOverviewActivity owner = mOwner.get();
      if (owner!=null && ! owner.myIsDestroyed()) {
        owner.updateSyncStatus();
        sendEmptyMessageDelayed(1, 5000);// wait 5 seconds and send again
      }
    }
  }

  private boolean myIsDestroyed() {
    return mIsDestroyed;
  }

  private MyRefreshHandler mRefreshHandler;
  private SyncStatusObserverData mProcessSyncObserverHandle;
  private SyncStatusObserverData mTaskSyncObserverHandle;
  private boolean mIsDestroyed = true;

  private void updateSyncStatus() {
    ProcessSyncManager    syncManager     = getSyncManager();
    DebugOverviewActivity owner           = this;

    String processesStatus = "Process Sync: " + (syncManager.isProcessModelSyncPending() ? "Pending" : (syncManager.isProcessModelSyncActive() ? "Active" : "Rest"));
    String taskStatus = "\nTask Sync: " + (syncManager.isTaskSyncPending() ? "Pending" : (syncManager.isTaskSyncActive() ? "Active" : "Rest"));

    owner.binding.setDebug(processesStatus + taskStatus+ '\n'+iteration++);
  }

  private ActivityOverviewDebugBinding binding;
  private int iteration=1;

  protected ActivityOverviewBinding bindLayout() {
    binding =  DataBindingUtil.setContentView(this, R.layout.activity_overview_debug);
    binding.debugText.setOnClickListener(this);
    return binding.normalOverview;
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SyncStatusObserver mySyncObserver = new MySyncObserver(this);

    ProcessSyncManager syncManager = getSyncManager();
    mProcessSyncObserverHandle = syncManager.addOnStatusChangeObserver(ProcessModelProvider.AUTHORITY, mySyncObserver);
    mTaskSyncObserverHandle = syncManager.addOnStatusChangeObserver(TaskProvider.AUTHORITY, mySyncObserver);
//
//    mRefreshHandler = new MyRefreshHandler(this);
    mIsDestroyed = false;
  }

  @Override
  protected void onResume() {
    super.onResume();
//    mRefreshHandler.resume();
  }

  @Override
  protected void onPause() {
//    mRefreshHandler.pause();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    mIsDestroyed = true;
//    mRefreshHandler.pause();
//    mRefreshHandler = null;
//
    ProcessSyncManager syncManager = getSyncManager();
    syncManager.removeOnStatusChangeObserver(mProcessSyncObserverHandle);
    syncManager.removeOnStatusChangeObserver(mTaskSyncObserverHandle);
    super.onDestroy();
  }

  @Override
  public void onClick(final View v) {
    updateSyncStatus();
  }
}
