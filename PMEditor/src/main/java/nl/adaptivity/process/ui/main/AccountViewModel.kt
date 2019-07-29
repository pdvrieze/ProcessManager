/*
 * Copyright (c) 2018.
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

package nl.adaptivity.process.ui.main

import android.accounts.Account
import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import nl.adaptivity.android.coroutines.CoroutineActivity
import nl.adaptivity.android.coroutines.aAsync
import nl.adaptivity.android.coroutines.aLaunch
import nl.adaptivity.android.darwin.ensureAccount
import java.net.URI

class AccountViewModel(): ViewModel() {
    val account: LiveData<Account?> = MutableLiveData()

    fun requestAccount(activity: CoroutineActivity, authBase: URI?) {

        activity.aLaunch {
            val a = ensureAccount(authBase)
            a.flatMap { (account as MutableLiveData).postValue(it) }
        }
    }
}
