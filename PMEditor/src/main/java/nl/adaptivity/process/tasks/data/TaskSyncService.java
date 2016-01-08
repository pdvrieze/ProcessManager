package nl.adaptivity.process.tasks.data;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class TaskSyncService extends Service {

  // Storage for an instance of the sync adapter
  private static TaskSyncAdapter sSyncAdapter = null;
  // Object to use as a thread-safe lock
  private static final Object sSyncAdapterLock = new Object();
  /*
   * Instantiate the sync adapter object.
   */
  @Override
  public void onCreate() {
      synchronized (sSyncAdapterLock) {
          if (sSyncAdapter == null) {
              sSyncAdapter = new TaskSyncAdapter(getApplicationContext());
          }
      }
  }

  /**
   * Return an object that allows the system to invoke
   * the sync adapter.
   *
   */
  @Override
  public IBinder onBind(Intent intent) {
      return sSyncAdapter.getSyncAdapterBinder();
  }

}
