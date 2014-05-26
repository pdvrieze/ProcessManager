package nl.adaptivity.process.models;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class ProcessModelSyncService extends Service {

  // Storage for an instance of the sync adapter
  private static ProcessModelSyncAdapter sSyncAdapter = null;
  // Object to use as a thread-safe lock
  private static final Object sSyncAdapterLock = new Object();
  /*
   * Instantiate the sync adapter object.
   */
  @Override
  public void onCreate() {
      synchronized (sSyncAdapterLock) {
          if (sSyncAdapter == null) {
              sSyncAdapter = new ProcessModelSyncAdapter(getApplicationContext());
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
