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

package nl.adaptivity.android.compat

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity


abstract class TitleFragment : Fragment() {


    protected abstract fun getTitle(context: Context): CharSequence

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is FragmentActivity) {
            val titleViewModel = ViewModelProviders.of(activity).get(TitleViewModel::class.java)
            titleViewModel.setTitle(getTitle(activity))
        }
    }
}

class TitleViewModel: ViewModel() {
    private val _title = MutableLiveData<CharSequence>()

    val title: LiveData<CharSequence> get() = _title

    fun setTitle(newTitle: CharSequence) {
        _title.postValue(newTitle)
    }
}