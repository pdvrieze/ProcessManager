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

package nl.adaptivity.process.ui.model

import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.app.NavUtils
import android.view.MenuItem
import android.widget.Toast
import nl.adaptivity.android.darwin.AuthenticatedWebClientFactory
import nl.adaptivity.android.util.GetNameDialogFragment
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.models.ProcessModelProvider
import nl.adaptivity.process.ui.ProcessSyncManager
import nl.adaptivity.process.ui.main.OverviewActivity
import nl.adaptivity.process.ui.main.ProcessBaseActivity
import nl.adaptivity.process.ui.main.SettingsActivity
import nl.adaptivity.process.ui.model.ProcessModelDetailFragment.ProcessModelDetailFragmentCallbacks


/**
 * An activity representing a single ProcessModel detail screen. This activity
 * is only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a
 * [ProcessModelListOuterFragment].
 *
 *
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a [ProcessModelDetailFragment].
 */
class ProcessModelDetailActivity : ProcessBaseActivity(), ProcessModelDetailFragmentCallbacks, GetNameDialogFragmentCallbacks {
    private var mModelHandleToInstantiate: Long = 0
    private var mSyncManager: ProcessSyncManager? = null

    override val syncManager: ProcessSyncManager?
        get() {
            val account = account
            if (account == null) {
                mSyncManager = null
            } else if (mSyncManager == null) {
                mSyncManager = ProcessSyncManager(this, AuthenticatedWebClientFactory.getStoredAccount(this))
            }
            return mSyncManager
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_processmodel_detail)

        // Show the Up button in the action bar.
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val arguments = Bundle()
            arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID,
                              intent.getLongExtra(ProcessModelDetailFragment.ARG_ITEM_ID, -1))
            val fragment = ProcessModelDetailFragment()
            fragment.arguments = arguments
            supportFragmentManager.beginTransaction()
                    .add(R.id.processmodel_detail_container, fragment)
                    .commit()
        }

        requestAccount(SettingsActivity.getAuthBase(this))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, Intent(this, OverviewActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onProcessModelSelected(processModelRowId: Long) {
        if (processModelRowId >= 0) {
            val intent = Intent(this, ProcessModelDetailActivity::class.java)
            intent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, processModelRowId)
            startActivity(intent)
        }
        finish()
    }

    override fun onInstantiateModel(modelHandle: Long, suggestedName: String) {
        mModelHandleToInstantiate = modelHandle
        GetNameDialogFragment.show(supportFragmentManager, DLG_MODEL_INSTANCE_NAME, "Instance name",
                                   "Provide a name for the process instance", this, suggestedName)
    }

    override fun onNameDialogCompletePositive(dialog: GetNameDialogFragment, id: Int, name: String) {
        try {
            ProcessModelProvider.instantiate(this, mModelHandleToInstantiate, name)
        } catch (e: RemoteException) {
            Toast.makeText(this, "Unfortunately the process could not be instantiated: " + e.message,
                           Toast.LENGTH_SHORT).show()
        }

    }

    override fun onNameDialogCompleteNegative(dialog: GetNameDialogFragment, id: Int) {
        mModelHandleToInstantiate = -1L
    }

    companion object {

        private val DLG_MODEL_INSTANCE_NAME = 1
    }
}
