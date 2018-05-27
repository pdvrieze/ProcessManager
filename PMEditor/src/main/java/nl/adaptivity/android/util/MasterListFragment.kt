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

package nl.adaptivity.android.util

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import nl.adaptivity.android.recyclerview.SelectableAdapter
import nl.adaptivity.process.ui.ProcessSyncManager
import nl.adaptivity.sync.SyncManager


open class MasterListFragment<M : SyncManager> : RecyclerFragment() {

    /**
     * A dummy implementation of the [ListCallbacks] interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    val sDummyCallbacks = object : ListCallbacks<ProcessSyncManager> {
        internal var context = this@MasterListFragment.context?.applicationContext

        override val isTwoPane: Boolean
            get() = false

        override val syncManager: ProcessSyncManager?
            get() = ProcessSyncManager(context, null)

        override fun onItemClicked(row: Int, id: Long): Boolean {/*dummy, not processed*/
            return false
        }

        override fun onItemSelected(row: Int, id: Long) {/*dummy*/
        }
    }

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private var mCallbacks: ListCallbacks<M> = sDummyCallbacks as ListCallbacks<M>

    protected val callbacks: ListCallbacks<M>
        get() {
            if (mCallbacks === sDummyCallbacks) {
                var parent = parentFragment
                while (parent != null) {
                    if (parent is ListCallbacks<*>) {
                        mCallbacks = parent as ListCallbacks<M>
                        return mCallbacks
                    }
                    parent = parent.parentFragment
                }
            }
            return mCallbacks
        }

    init {
        Log.i(MasterListFragment::class.java.simpleName, "Creating a new instanceo of " + javaClass.simpleName)
    }

    interface ListCallbacks<M : SyncManager> {
        val isTwoPane: Boolean
        val syncManager: M?
        /** Initiate a click. When this returns true, further ignore the event (don't select)  */
        fun onItemClicked(row: Int, id: Long): Boolean

        fun onItemSelected(row: Int, id: Long)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        callbacks
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        if (mCallbacks === sDummyCallbacks) {
            if (getActivity() is ListCallbacks<*>) {
                mCallbacks = getActivity() as ListCallbacks<M>
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_ID)) {
            setActivatedId(savedInstanceState.getLong(STATE_ACTIVATED_ID))
        } else {
            val arguments = arguments
            if (arguments != null && arguments.containsKey(MasterDetailOuterFragment.ARG_ITEM_ID)) {
                setActivatedId(arguments.getLong(MasterDetailOuterFragment.ARG_ITEM_ID))
            }
        }
    }

    override fun onDetach() {
        super.onDetach()

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks as ListCallbacks<M>
    }

    protected fun doOnItemClicked(position: Int, nodeId: Long): Boolean {
        return callbacks.onItemClicked(position, nodeId)
    }

    protected fun doOnItemSelected(position: Int, nodeId: Long) {
        callbacks.onItemSelected(position, nodeId)
    }

    protected fun setActivatedId(id: Long) {
        val vh = recyclerView!!.findViewHolderForItemId(id)
        val adapter = listAdapter
        if (adapter is SelectableAdapter<*>) {
            val selectableAdapter = adapter as SelectableAdapter<*>
            if (vh != null) {
                selectableAdapter.setSelectedItem(vh.adapterPosition.toLong())
            } else {
                selectableAdapter.setSelectedItem(id)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val adapter = listAdapter
        if (adapter is SelectableAdapter<*>) {
            val selectableAdapter = adapter as SelectableAdapter<*>

            if (selectableAdapter.selectedId != RecyclerView.NO_ID) {
                // Serialize and persist the activated item position.
                outState.putLong(STATE_ACTIVATED_ID, selectableAdapter.selectedId)
            }
        }
    }

    companion object {

        /**
         * The serialization (saved instance state) Bundle key representing the
         * activated item position. Only used on tablets.
         */
        val STATE_ACTIVATED_ID = "activated_id"
    }

}
