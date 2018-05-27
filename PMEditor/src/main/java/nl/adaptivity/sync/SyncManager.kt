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

package nl.adaptivity.sync

import android.accounts.Account
import android.content.ContentResolver
import android.content.SyncStatusObserver
import android.os.Debug
import android.util.Log
import nl.adaptivity.process.editor.android.BuildConfig
import java.util.*


/**
 * Created by pdvrieze on 28/02/16.
 */
open class SyncManager(val account: Account?, private val authorities: Array<String>) {
    private val syncObservers = ArrayList<SyncStatusObserverData>(2)
    private val syncObserver = SyncStatusObserver { which -> onInnerSyncStatusChanged(which) }
    private var syncObserverHandle: Any? = null

    val activeSyncTargets: List<String>
        get() {
            val result = ArrayList<String>(2)
            synchronized(this) {
                for (authority in authorities) {
                    if (isSyncable(authority)) {
                        result.add(authority)
                    }
                }
            }
            return result
        }

    private fun onInnerSyncStatusChanged(which: Int) {
        if (account != null) {
            synchronized(this) {
                for (authority in authorities) {
                    if (ContentResolver.isSyncActive(account, authority) || ContentResolver.isSyncPending(account,
                                                                                                          authority)) {
                        for (observerData in syncObservers) {
                            if (authority == observerData.authority) {
                                observerData.observer.onStatusChanged(which)
                            }
                        }
                    }
                }
            }
        }
    }

    fun addOnStatusChangeObserver(authority: String,
                                  syncObserver: SyncStatusObserver): SyncStatusObserverData {
        val data: SyncStatusObserverData =
        synchronized(this) {
            if (syncObserverHandle == null && isSyncable(authority)) {
                Log.d(TAG, "TRACE: OverallSyncObserver")
                syncObserverHandle = ContentResolver.addStatusChangeListener(
                        ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE or ContentResolver.SYNC_OBSERVER_TYPE_PENDING,
                        this.syncObserver)
            }
            SyncStatusObserverData(authority, syncObserver).also { data ->
                Log.d(TAG, "TRACE: SyncObserver_$authority")

                syncObservers.add(data)
            }
        }
        return data
    }

    fun isSyncable(authority: String): Boolean {
        return if (account == null || DISABLEONDEBUG && BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
            false
        } else ContentResolver.getIsSyncable(
                account, authority) > 0
    }

    fun removeOnStatusChangeObserver(handle: SyncStatusObserverData) {
        Log.d(TAG, "TRACE: Remove SyncObserver_" + handle.authority)
        synchronized(this) {
            if (syncObserverHandle != null && syncObservers.remove(handle) && syncObservers.isEmpty()) {
                ContentResolver.removeStatusChangeListener(syncObserverHandle)
                Log.d(TAG, "TRACE: Remove OverallSyncObserver")
                syncObserverHandle = null
            }
        }
    }

    fun verifyNoObserversActive() {
        synchronized(this) {
            for (syncStatusObserverData in syncObservers) {
                Log.w(TAG, "Synchronization observer active for " + syncStatusObserverData.authority)
            }
        }
        if (syncObserverHandle != null) {
            Log.w(TAG, "Overall synchronization observer still active", IllegalStateException())
        }
    }

    inner class SyncStatusObserverData(internal val authority: String, internal val observer: SyncStatusObserver)

    companion object {

        private val TAG = "SyncManager"
        private val DISABLEONDEBUG = false
    }
}
