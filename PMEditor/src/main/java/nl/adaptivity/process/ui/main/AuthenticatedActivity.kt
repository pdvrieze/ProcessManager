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
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.net.URI


/**
 * Created by pdvrieze on 11/01/16.
 */
abstract class AuthenticatedActivity : AppCompatActivity() {

    fun getAccount(): LiveData<Account?> = ViewModelProviders.of(this).get(AccountViewModel::class.java).account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewModelProviders.of(this).get(AccountViewModel::class.java).account.observe(this, Observer { doAccountDetermined(it) })

    }

    protected open fun doAccountDetermined(account: Account?) {
        // Hook for subclasses
    }

    fun requestAccount(authBase: URI) {
        ViewModelProviders.of(this).get(AccountViewModel::class.java).requestAccount(this, authBase)
    }

}
