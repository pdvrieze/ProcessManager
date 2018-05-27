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

package nl.adaptivity.process.ui.model

import android.accounts.Account
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import net.devrieze.util.Handle
import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.sync.SyncState

class ProcessesViewModel: ViewModel() {

    val processes: LiveData<List<Handle<DrawableProcessModel>>> = TODO("Implement")

    val syncState: LiveData<SyncState> = MutableLiveData<SyncState>()

    fun sync(account: Account) {

    }
}