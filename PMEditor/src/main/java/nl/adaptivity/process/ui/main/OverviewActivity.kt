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
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.AsyncTask
import android.os.Bundle
import android.os.RemoteException
import android.support.annotation.IdRes
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.widget.Toast
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import nl.adaptivity.android.compat.TitleViewModel
import nl.adaptivity.android.util.GetNameDialogFragment
import nl.adaptivity.android.util.GetNameDialogFragment.GetNameDialogFragmentCallbacks
import nl.adaptivity.android.util.MasterDetailOuterFragment
import nl.adaptivity.process.data.ProviderHelper
import nl.adaptivity.process.editor.android.R
import nl.adaptivity.process.editor.android.databinding.ActivityOverviewBinding
import nl.adaptivity.process.models.ProcessModelProvider
import nl.adaptivity.process.tasks.data.TaskProvider
import nl.adaptivity.process.ui.main.OverviewFragment.OverviewCallbacks
import nl.adaptivity.process.ui.model.ProcessModelDetailFragment.ProcessModelDetailFragmentCallbacks
import nl.adaptivity.process.ui.task.TaskDetailFragment.TaskDetailCallbacks


class OverviewActivity : ProcessBaseActivity(), OverviewCallbacks, GetNameDialogFragmentCallbacks, ProcessModelDetailFragmentCallbacks, TaskDetailCallbacks {

    private lateinit var binding: ActivityOverviewBinding

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private var mModelIdToInstantiate = -1L


    protected open fun bindLayout(): ActivityOverviewBinding {
        return DataBindingUtil.setContentView(this, R.layout.activity_overview)
    }

    private lateinit var currentDestination: LiveData<NavDestination>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        supportFragmentManager.addOnBackStackChangedListener(this)
        binding = bindLayout()

        val titleViewModel = ViewModelProviders.of(this).get(TitleViewModel::class.java)
        titleViewModel.title.observe(this, Observer {
            val ab = supportActionBar
            if (ab != null) {
                ab.title = it
            }
            invalidateOptionsMenu()
        })

        val toolbar = binding.overviewAppBar!!.toolbar

        setSupportActionBar(toolbar)

        val drawer = binding.overviewDrawer

        drawerToggle = ActionBarDrawerToggle(this, drawer, toolbar,
                                             R.string.navigation_drawer_open,
                                             R.string.navigation_drawer_close)
        drawer.addDrawerListener(drawerToggle)

        val navigationView = binding.navView

        val navController = navController()

        navigationView.setupWithNavController(navController)

        currentDestination = MutableLiveData<NavDestination>().also { data ->
            navController.addOnNavigatedListener { c, d -> data.postValue(d) }
            data.observe(this, Observer { d ->
                d?.let { binding.navView.setCheckedItem(destIdToNavId(it.id)) }
            })
        }

        requestAccount(ProviderHelper.getAuthBase(this))

        if (savedInstanceState == null) {
            val intent = intent
            val uri = intent.data
            var handle: Long = 0
            @IdRes var navTarget = R.id.nav_home
            if (uri != null) {
                val path = uri.path
                if (path!!.startsWith(SERVERPATH_MODELS)) {
                    navTarget = R.id.nav_models
                    if (path.length > SERVERPATH_MODELS.length && path[SERVERPATH_MODELS.length] == '/') {
                        handle = java.lang.Long.parseLong(path.substring(SERVERPATH_MODELS.length + 1))
                    }
                } else if (path.startsWith(SERVERPATH_TASKS)) {
                    navTarget = R.id.nav_tasks
                    if (path.length > SERVERPATH_TASKS.length && path[SERVERPATH_TASKS.length] == '/') {
                        handle = java.lang.Long.parseLong(path.substring(SERVERPATH_TASKS.length + 1))
                    }
                }
            }
            if (handle != 0L) {
                // TODO don't use this task, but use viewmodels/livedata
                val bgNavigation = NavigationTask()
                bgNavigation.execute(java.lang.Long.valueOf(navTarget.toLong()), java.lang.Long.valueOf(handle))
            }
        }

    }

    override fun onDestroy() {
//        supportFragmentManager.removeOnBackStackChangedListener(this)
        super.onDestroy()

    }

    override fun onSupportNavigateUp() = navController().navigateUp()

    @IdRes
    fun destIdToNavId(@IdRes destId: Int): Int {
        return when (destId) {
            R.id.tasklistFragment  -> R.id.nav_tasks
            R.id.modellistFragment -> R.id.nav_models
            R.id.overviewFragment  -> R.id.nav_home
            else                   -> -1
        }
    }

    private fun navController() = findNavController(R.id.overview_container)

/*
    override fun onBackStackChanged() {
        val fm = supportFragmentManager
        val currentFragment = fm.findFragmentById(R.id.overview_container)
        var navId = -1
        if (currentFragment is OverviewFragment) {
            navId = R.id.nav_home
        } else if (currentFragment is ProcessModelListOuterFragment) {
            navId = R.id.nav_models
        } else if (currentFragment is TaskListOuterFragment) {
            navId = R.id.nav_tasks
        }
        if (navId >= 0) {
            binding.navView.setCheckedItem(navId)
        }
    }
*/

    override fun doAccountDetermined(account: Account?) {
        if (account != null) {
            ProviderHelper.requestSync(this, ProcessModelProvider.AUTHORITY, true)
            ProviderHelper.requestSync(this, TaskProvider.AUTHORITY, true)
        }
    }

    override fun onBackPressed() {
        if (binding.overviewDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.overviewDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState()
    }

    override fun showTasksFragment() {
        navController().navigate(R.id.nav_tasks)
    }

    override fun showModelsFragment() {
        navController().navigate(R.id.nav_models)
    }

    override fun onShowTask(taskId: Long) {
        // TODO just replace child if already there
        val args = Bundle(1).also { MasterDetailOuterFragment.addArgs(it, taskId) }
        navController().navigate(R.id.nav_models, args)
    }

    override fun onProcessModelSelected(processModelId: Long) {
        // TODO just replace child if already there
        val args = Bundle(1).also { MasterDetailOuterFragment.addArgs(it, processModelId) }
        navController().navigate(R.id.nav_models, args)
    }


    private fun onNavigationItemSelected(@IdRes id: Int, addToBackstack: Boolean, itemId: Long = 0): Boolean {
        val navController = navController()
        when (id) {
            R.id.nav_home     -> if (navController.currentDestination.id != R.id.overview_container) {
                navController.navigate(R.id.overview_container)
            }
            R.id.nav_tasks    -> if (navController.currentDestination.id != R.id.tasklistFragment) {
                navController.navigate(R.id.nav_tasks)
            }
            R.id.nav_models   -> navController.navigate(R.id.nav_models)
            R.id.nav_share    -> {
            }
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        val drawer = binding.overviewDrawer
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onNameDialogCompletePositive(dialog: GetNameDialogFragment, id: Int, name: String) {
        try {
            ProcessModelProvider.instantiate(this, mModelIdToInstantiate, name)
        } catch (e: RemoteException) {
            Toast.makeText(this, "Unfortunately the process could not be instantiated: " + e.message,
                           Toast.LENGTH_SHORT).show()
        }

    }

    override fun onNameDialogCompleteNegative(dialog: GetNameDialogFragment, id: Int) {
        mModelIdToInstantiate = -1L
    }

    override fun requestSyncProcessModelList(immediate: Boolean, minAge: Long) {
        syncManager?.requestSyncProcessModelList(immediate, minAge)
    }

    override fun requestSyncTaskList(immediate: Boolean, minAge: Long) {
        syncManager?.requestSyncTaskList(immediate, minAge)
    }

    override fun onInstantiateModel(modelId: Long, suggestedName: String) {
        mModelIdToInstantiate = modelId
        GetNameDialogFragment.show(supportFragmentManager, DLG_MODEL_INSTANCE_NAME, "Instance name",
                                   "Provide a name for the process instance", this, suggestedName)

    }

    override fun dismissTaskDetails() {
/*
        val af = activeFragment
        if (af is TaskListOuterFragment) {
            af.onItemSelected(-1, -1)
        }
*/
    }

    companion object {

        private const val TAG = "OverviewActivity"
        private const val DLG_MODEL_INSTANCE_NAME = 1
        const val SERVERPATH_MODELS = "/ProcessEngine/processModels"
        const val SERVERPATH_TASKS = "/PEUserMessageHandler/UserMessageService/pendingTasks"
    }


    inner class NavigationTask : AsyncTask<Long, Void, Long>() {
        var mNavTarget: Int = 0

        protected override fun doInBackground(vararg params: Long?): Long {
            mNavTarget = params[0]!!.toInt()
            val handle = params[1]!!
            val id: Long
            when (mNavTarget) {
                R.id.nav_models -> id = ProcessModelProvider.getIdForHandle(this@OverviewActivity, handle)
                R.id.nav_tasks  -> id = TaskProvider.getIdForHandle(this@OverviewActivity, handle)
                else            -> return java.lang.Long.valueOf(0)
            }

            return java.lang.Long.valueOf(id)
        }

        override fun onPostExecute(itemId: Long) {
            onNavigationItemSelected(mNavTarget, true, itemId)
            binding.navView.setCheckedItem(mNavTarget)
        }
    }


}
