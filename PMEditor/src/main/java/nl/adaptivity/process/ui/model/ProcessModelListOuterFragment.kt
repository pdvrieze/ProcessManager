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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import nl.adaptivity.android.util.MasterDetailOuterFragment
import nl.adaptivity.android.util.MasterListFragment.ListCallbacks
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.ui.ProcessSyncManager
import nl.adaptivity.process.ui.model.ProcessModelDetailFragment.ProcessModelDetailFragmentCallbacks
import nl.adaptivity.sync.SyncManager


/**
 * An activity representing a list of ProcessModels. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * [ProcessModelDetailActivity] representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 *
 *
 * The activity makes heavy use of fragments. The list of items is a
 * [ProcessModelListFragment] and the item details (if present) is a
 * [ProcessModelDetailFragment].
 *
 *
 * This activity also implements the required
 * [ListCallbacks] interface to listen for item
 * selections.
 */
class ProcessModelListOuterFragment constructor() : MasterDetailOuterFragment(R.layout.outer_processmodel_list,
                                                                R.id.processmodel_list_container,
                                                                R.id.processmodel_detail_container), ListCallbacks<SyncManager>, ProcessModelDetailFragmentCallbacks {

    private var mCallbacks: ProcessModelListCallbacks? = null

    interface ProcessModelListCallbacks {

        val syncManager: ProcessSyncManager?

        fun requestSyncProcessModelList(immediate: Boolean, minAge: Long)

        fun onInstantiateModel(id: Long, suggestedName: String)
    }

    public override fun createListFragment(args: Bundle?): ProcessModelListFragment {
        val listFragment = ProcessModelListFragment()
        if (args != null) {
            listFragment.arguments = args
        }
        return listFragment
    }

    @Suppress("DEPRECATION")
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is ProcessModelListCallbacks) {
            mCallbacks = activity
        }
    }

    override fun onStart() {
        super.onStart()
        if (mCallbacks != null) {
            mCallbacks!!.requestSyncProcessModelList(true, ProcessSyncManager.DEFAULT_MIN_AGE)
        }
    }

    /**
     * Callback method from [ListCallbacks] indicating
     * that the item with the given ID was selected.
     */
    override fun onProcessModelSelected(processModelId: Long) {
        if (isTwoPane) {
            if (processModelId >= 0) {
                // In two-pane mode, show the detail view in this activity by
                // adding or replacing the detail fragment using a
                // fragment transaction.
                val arguments = Bundle()
                arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID, processModelId)
                val fragment = ProcessModelDetailFragment()
                fragment.arguments = arguments
                childFragmentManager.beginTransaction()
                    .replace(R.id.processmodel_detail_container, fragment)
                    .commit()
            } else {
                val frag = childFragmentManager.findFragmentById(R.id.processmodel_detail_container)
                if (frag != null) {
                    fragmentManager!!.beginTransaction()
                        .remove(frag)
                        .commit()
                }
            }

        } else {
            if (processModelId >= 0) {
                // In single-pane mode, simply start the detail activity
                // for the selected item ID.
                val detailIntent = Intent(activity, ProcessModelDetailActivity::class.java)
                detailIntent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, processModelId)
                startActivity(detailIntent)
            }
        }
    }

    override val syncManager: ProcessSyncManager?
        get() {
            return mCallbacks!!.syncManager
        }

    override fun createDetailFragment(itemId: Long): ProcessModelDetailFragment {
        val fragment = ProcessModelDetailFragment()
        val arguments = Bundle()
        arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID, itemId)
        fragment.arguments = arguments
        return fragment
    }

    override fun getDetailIntent(itemId: Long): Intent {
        val detailIntent = Intent(activity, ProcessModelDetailActivity::class.java)
        detailIntent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, itemId)
        return detailIntent
    }

    override fun getTitle(context: Context): CharSequence {
        return context.getString(R.string.title_processmodel_list)
    }

    override fun onInstantiateModel(modelId: Long, suggestedName: String) {
        mCallbacks!!.onInstantiateModel(modelId, suggestedName)
    }

    companion object {

        fun newInstance(modelId: Long): ProcessModelListOuterFragment {
            val result = ProcessModelListOuterFragment()
            if (modelId != 0L) {
                result.arguments = MasterDetailOuterFragment.addArgs(null, modelId)
            }
            return result
        }
    }
}
